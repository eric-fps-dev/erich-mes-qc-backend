package com.fps.svmes.services.impl;

import com.fps.svmes.dto.LegacyMigrationResult;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.repositories.mongoRepo.ApprovalInstanceRepository;
import com.fps.svmes.services.ApprovalInstanceBackfillService;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.QcFormTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalInstanceBackfillServiceImpl implements ApprovalInstanceBackfillService {

    private static final int BATCH_SIZE = 200;
    private static final String FORM_COLLECTION_PREFIX = "form_template_";

    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceRepository approvalInstanceRepository;
    private final QcFormTemplateService qcFormTemplateService;
    private final ApprovalInstanceService approvalInstanceService;

    @Override
    public LegacyMigrationResult backfillMissingApprovalInstances() {
        int totalProcessed = 0;
        int totalMigrated = 0;
        int totalSkipped = 0;
        int totalFailed = 0;
        List<LegacyMigrationResult.FailedRecord> failures = new ArrayList<>();

        List<String> formCollections = mongoTemplate.getCollectionNames().stream()
                .filter(name -> name.startsWith(FORM_COLLECTION_PREFIX))
                .sorted()
                .toList();

        log.info("Approval-instance backfill starting: {} form collections found", formCollections.size());

        for (String collectionName : formCollections) {
            ObjectId lastSeenId = null;
            while (true) {
                List<Document> batch = fetchBatch(collectionName, lastSeenId);
                if (batch.isEmpty()) {
                    break;
                }

                for (Document submission : batch) {
                    totalProcessed++;
                    String submissionId = submission.getObjectId("_id").toString();
                    try {
                        if (pairIfMissing(submission, collectionName)) {
                            totalMigrated++;
                        } else {
                            totalSkipped++;
                        }
                    } catch (Exception e) {
                        totalFailed++;
                        failures.add(new LegacyMigrationResult.FailedRecord(collectionName, submissionId, e.getMessage()));
                        log.error("Approval-instance backfill failed for submission {} in {}", submissionId, collectionName, e);
                    }
                    lastSeenId = submission.getObjectId("_id");
                }

                if (batch.size() < BATCH_SIZE) {
                    break;
                }
            }
        }

        log.info("Approval-instance backfill complete - processed={} migrated={} skipped={} failed={}",
                totalProcessed, totalMigrated, totalSkipped, totalFailed);
        return new LegacyMigrationResult(totalProcessed, totalMigrated, totalSkipped, totalFailed, failures);
    }

    private boolean pairIfMissing(Document submission, String collectionName) {
        String submissionId = submission.getObjectId("_id").toString();
        if (approvalInstanceRepository.findByFormSubmissionIdAndFormSubmissionCollectionName(submissionId, collectionName).isPresent()) {
            return false;
        }

        Long formTemplateId = resolveFormTemplateId(submission, collectionName);
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(formTemplateId);
        String approvalTemplateId = template.getApprovalTemplateId();
        Long createdBy = asLong(submission.get("created_by"));

        approvalInstanceService.create(submissionId, collectionName, formTemplateId, approvalTemplateId, createdBy);
        return true;
    }

    private List<Document> fetchBatch(String collectionName, ObjectId lastSeenId) {
        Query query = new Query();
        if (lastSeenId != null) {
            query.addCriteria(Criteria.where("_id").gt(lastSeenId));
        }
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(BATCH_SIZE);
        return mongoTemplate.find(query, Document.class, collectionName);
    }

    private Long resolveFormTemplateId(Document submission, String collectionName) {
        Long formTemplateId = asLong(submission.get("form_template_id"));
        if (formTemplateId != null) {
            return formTemplateId;
        }
        String[] parts = collectionName.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid form collection name: " + collectionName);
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid form template ID in collection name: " + collectionName, e);
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
