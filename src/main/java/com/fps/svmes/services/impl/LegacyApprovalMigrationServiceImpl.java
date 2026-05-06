package com.fps.svmes.services.impl;

import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.enums.approval.ApprovalAction;
import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.enums.form.ApprovalModel;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.models.nosql.approval.ApprovalActionLog;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceFilterSnapshot;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;
import com.fps.svmes.repositories.mongoRepo.ApprovalInstanceRepository;
import com.fps.svmes.services.LegacyApprovalMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LegacyApprovalMigrationServiceImpl implements LegacyApprovalMigrationService {

    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceRepository approvalInstanceRepository;

    @Value("${qc.legacy-migration.leader-role-id:6}")
    private String leaderRoleId;

    @Value("${qc.legacy-migration.supervisor-role-id:7}")
    private String supervisorRoleId;

    @Value("${qc.legacy-migration.flow2-approval-template-id:69e7c31db26b380d82fae8e8}")
    private String flow2TemplateId;

    @Value("${qc.legacy-migration.flow3-approval-template-id:69e7c32db26b380d82fae8e9}")
    private String flow3TemplateId;

    @Value("${qc.legacy-migration.flow4-approval-template-id:68e699a946d9997100ff644a}")
    private String flow4TemplateId;

    private static final int BATCH_SIZE = 200;
    private static final String FORM_COLLECTION_PREFIX = "form_template_";
    private static final Set<String> SENTINEL_ROLES = Set.of("submitter", "archive");

    @Override
    public LegacyMigrationResult migrate() {
        int totalProcessed = 0;
        int totalMigrated = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        List<LegacyMigrationResult.FailedRecord> failures = new ArrayList<>();

        List<String> formCollections = mongoTemplate.getCollectionNames().stream()
                .filter(name -> name.startsWith(FORM_COLLECTION_PREFIX))
                .sorted()
                .toList();

        log.info("Legacy approval migration starting: {} form collections found", formCollections.size());

        for (String collection : formCollections) {
            Long formTemplateId = parseFormTemplateId(collection);
            if (formTemplateId == null) {
                log.warn("Skipping collection with unparseable template ID: {}", collection);
                continue;
            }

            while (true) {
                List<Document> batch = fetchLegacyBatch(collection, BATCH_SIZE);
                if (batch.isEmpty()) break;

                for (Document doc : batch) {
                    totalProcessed++;
                    String submissionId = doc.getObjectId("_id").toString();
                    try {
                        MigrationOutcome outcome = migrateOne(doc, submissionId, collection, formTemplateId);
                        if (outcome == MigrationOutcome.MIGRATED || outcome == MigrationOutcome.RECOVERED) totalMigrated++;
                        else totalSkipped++;
                    } catch (Exception e) {
                        totalFailed++;
                        failures.add(new LegacyMigrationResult.FailedRecord(collection, submissionId, e.getMessage()));
                        log.error("Migration failed for submission {} in {}", submissionId, collection, e);
                    }
                }

                if (batch.size() < BATCH_SIZE) break;
            }
        }

        log.info("Legacy approval migration complete — processed={} migrated={} skipped={} failed={}",
                totalProcessed, totalMigrated, totalSkipped, totalFailed);
        return new LegacyMigrationResult(totalProcessed, totalMigrated, totalSkipped, totalFailed, failures);
    }

    // ── per-document migration ──────────────────────────────────────────────

    private MigrationOutcome migrateOne(Document doc, String submissionId, String collection, Long formTemplateId) {
        FormSubmissionState formState = inferFormState(realApprovalSteps(extractApprovalInfo(doc)), doc);

        if (approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionName(submissionId, collection).isPresent()) {
            if (ApprovalModel.V2_VALUE.equals(doc.getString(ApprovalModel.DOCUMENT_FIELD))) {
                return MigrationOutcome.SKIPPED;
            }
            stampFormDocument(submissionId, collection, formState);
            return MigrationOutcome.RECOVERED;
        }

        List<Document> rawApprovalInfo = extractApprovalInfo(doc);
        List<Document> realSteps = realApprovalSteps(rawApprovalInfo);

        List<ApprovalInstanceStep> instanceSteps = buildInstanceSteps(realSteps);
        int currentStepSequence = inferCurrentStepSequence(instanceSteps);
        String approvalTemplateId = inferApprovalTemplateId(realSteps);
        List<ApprovalActionLog> actionLog = collectActionLogs(instanceSteps);

        ApprovalInstance instance = new ApprovalInstance();
        instance.setFormSubmissionId(submissionId);
        instance.setFormSubmissionCollectionName(collection);
        instance.setApprovalTemplateId(approvalTemplateId);
        instance.setFormTemplateId(String.valueOf(formTemplateId));
        instance.setCurrentStepSequence(currentStepSequence);
        instance.setApprovalSteps(instanceSteps);
        instance.setActionLog(actionLog);
        instance.setVersionNumber(1);
        instance.setFilterSnapshot(buildFilterSnapshot(doc, formTemplateId, formState));
        instance.setStatus(1);

        Date docCreatedAt = doc.getDate("created_at");
        instance.setCreatedAt(docCreatedAt != null ? docCreatedAt.toInstant() : Instant.now());
        instance.setCreatedBy(asLong(doc.get("created_by")));

        approvalInstanceRepository.save(instance);
        stampFormDocument(submissionId, collection, formState);
        return MigrationOutcome.MIGRATED;
    }

    // ── approval_info parsing ───────────────────────────────────────────────

    private List<Document> extractApprovalInfo(Document doc) {
        Object raw = doc.get("approval_info");
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .toList();
    }

    /**
     * Filters out the "submitter" and "archive" sentinel nodes, leaving only real approver steps.
     */
    private List<Document> realApprovalSteps(List<Document> approvalInfo) {
        return approvalInfo.stream()
                .filter(step -> !SENTINEL_ROLES.contains(step.getString("role")))
                .toList();
    }

    // ── state inference ─────────────────────────────────────────────────────

    private FormSubmissionState inferFormState(List<Document> realSteps, Document doc) {
        String existingState = doc.getString("state");
        if (existingState != null && !existingState.isBlank() && !existingState.equals("void")) {
            try {
                return FormSubmissionState.fromValue(existingState);
            } catch (IllegalArgumentException ignored) {
                // unrecognised legacy state value — fall through to inference
            }
        }
        if (realSteps.isEmpty()) {
            // flow_1: no approver steps → auto-archived on creation
            return FormSubmissionState.ARCHIVED;
        }
        for (Document step : realSteps) {
            if ("pending".equals(step.getString("status"))) {
                return FormSubmissionState.UNDER_REVIEW;
            }
        }
        boolean allCompleted = realSteps.stream().allMatch(s -> "completed".equals(s.getString("status")));
        return allCompleted ? FormSubmissionState.ARCHIVED : FormSubmissionState.DRAFT;
    }

    // ── step construction ───────────────────────────────────────────────────

    private List<ApprovalInstanceStep> buildInstanceSteps(List<Document> realSteps) {
        List<ApprovalInstanceStep> result = new ArrayList<>();
        for (int i = 0; i < realSteps.size(); i++) {
            Document raw = realSteps.get(i);
            String role = raw.getString("role");
            String roleId = "leader".equals(role) ? leaderRoleId : supervisorRoleId;
            String stepName = "leader".equals(role) ? "QC Team Lead" : "QC Supervisor";

            ApprovalInstanceStep step = new ApprovalInstanceStep();
            step.setSequence(i);
            step.setStepName(stepName);
            step.setRequiredRoleId(roleId);
            step.setRequiredUserId(null);
            step.setRequiredType("role");
            step.setStepState(mapStepState(raw.getString("status")));
            step.setResetCounter(0);
            // Reconstruct audit record for completed steps from legacy approval_info data
            step.setLastActionRecord(buildStepActionLog(raw, i, roleId));
            result.add(step);
        }
        return result;
    }

    private ApprovalStepState mapStepState(String status) {
        return switch (status == null ? "" : status) {
            case "completed" -> ApprovalStepState.APPROVED;
            case "pending" -> ApprovalStepState.IN_PROGRESS;
            default -> ApprovalStepState.PENDING;
        };
    }

    private ApprovalActionLog buildStepActionLog(Document rawStep, int sequence, String roleId) {
        if (!"completed".equals(rawStep.getString("status"))) return null;
        Date timestamp = rawStep.getDate("timestamp");

        ApprovalActionLog log = new ApprovalActionLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setAction(ApprovalAction.APPROVED);
        log.setStepSequence(sequence);
        log.setComments(rawStep.getString("comments"));
        log.setESignature(rawStep.getString("e-signature"));
        log.setActorUserId(rawStep.get("user_id") != null ? rawStep.get("user_id").toString() : null);
        log.setActorRoleId(roleId);
        log.setActorRoleName(rawStep.getString("role"));
        log.setActedAt(timestamp != null ? timestamp.toInstant() : null);
        log.setFormSubmissionSnapshot(null);
        log.setFormTemplateSnapshot(null);
        return log;
    }

    /**
     * Collects the lastActionRecord from each APPROVED step as the instance-level action log.
     * Partial history only — the legacy system did not store submission or recall events.
     */
    private List<ApprovalActionLog> collectActionLogs(List<ApprovalInstanceStep> steps) {
        return steps.stream()
                .map(ApprovalInstanceStep::getLastActionRecord)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    // ── currentStepSequence ─────────────────────────────────────────────────

    /**
     * Mirrors the invariant maintained by ApprovalInstanceServiceImpl.approve():
     * - IN_PROGRESS step found → return its index
     * - All APPROVED (terminal) → return last step index (sequence is not incremented past the end)
     * - All PENDING (never submitted) → return 0
     */
    private int inferCurrentStepSequence(List<ApprovalInstanceStep> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStepState() == ApprovalStepState.IN_PROGRESS) return i;
        }
        boolean allApproved = !steps.isEmpty()
                && steps.stream().allMatch(s -> s.getStepState() == ApprovalStepState.APPROVED);
        return allApproved ? steps.size() - 1 : 0;
    }

    // ── template resolution ─────────────────────────────────────────────────

    private String inferApprovalTemplateId(List<Document> realSteps) {
        return switch (realSteps.size()) {
            case 0 -> null; // flow_1: auto-archived, no template
            case 1 -> "leader".equals(realSteps.get(0).getString("role")) ? flow2TemplateId : flow3TemplateId;
            default -> flow4TemplateId;
        };
    }

    // ── filter snapshot ─────────────────────────────────────────────────────

    private ApprovalInstanceFilterSnapshot buildFilterSnapshot(Document doc, Long formTemplateId, FormSubmissionState formState) {
        ApprovalInstanceFilterSnapshot snapshot = new ApprovalInstanceFilterSnapshot();
        snapshot.setFormTemplateId(formTemplateId);
        snapshot.setFormSubmissionState(formState.dbValue());
        snapshot.setFormSubmissionVersion(doc.get("version") instanceof Number n ? n.intValue() : 1);
        snapshot.setVersionGroupId(doc.getString("version_group_id"));
        snapshot.setCreatedAt(doc.getDate("created_at"));
        snapshot.setCreatedBy(asLong(doc.get("created_by")));
        snapshot.setRelatedInspectorIds(asLongList(doc.get("related_inspector_ids")));
        snapshot.setRelatedInspectors(doc.get("related_inspectors"));
        snapshot.setRelatedProductIds(asLongList(doc.get("related_product_ids")));
        snapshot.setRelatedProducts(doc.get("related_products"));
        snapshot.setRelatedBatchIds(asLongList(doc.get("related_batch_ids")));
        snapshot.setRelatedBatches(doc.get("related_batches"));
        snapshot.setRelatedTeamId(asLong(doc.get("related_team_id")));
        snapshot.setRelatedTeams(doc.get("related_teams"));
        snapshot.setRelatedShiftId(asLong(doc.get("related_shift_id")));
        snapshot.setRelatedShifts(doc.get("related_shifts"));
        return snapshot;
    }

    // ── form document stamping ───────────────────────────────────────────────

    private void stampFormDocument(String submissionId, String collection, FormSubmissionState state) {
        Update update = new Update()
                .set(ApprovalModel.DOCUMENT_FIELD, ApprovalModel.V2_VALUE)
                .set("state", state.dbValue());
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(new ObjectId(submissionId))),
                update,
                collection
        );
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private List<Document> fetchLegacyBatch(String collection, int limit) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where(ApprovalModel.DOCUMENT_FIELD).ne(ApprovalModel.V2_VALUE),
                Criteria.where("state").ne("void")
        )).limit(limit);
        return mongoTemplate.find(query, Document.class, collection);
    }

    private Long parseFormTemplateId(String collectionName) {
        String[] parts = collectionName.split("_");
        if (parts.length < 3) return null;
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Long> asLongList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::asLong).filter(Objects::nonNull).toList();
        }
        Long single = asLong(value);
        return single == null ? null : List.of(single);
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private enum MigrationOutcome { MIGRATED, RECOVERED, SKIPPED }
}
