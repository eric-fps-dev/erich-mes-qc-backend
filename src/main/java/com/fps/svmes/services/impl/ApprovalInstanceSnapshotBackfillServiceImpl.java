package com.fps.svmes.services.impl;

import com.fps.svmes.dto.ApprovalInstanceSnapshotBackfillResult;
import com.fps.svmes.services.ApprovalInstanceService;
import com.fps.svmes.services.ApprovalInstanceSnapshotBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalInstanceSnapshotBackfillServiceImpl implements ApprovalInstanceSnapshotBackfillService {

    private static final String APPROVAL_INSTANCE_COLLECTION = "qc-approval-instance";
    private static final String LEGACY_REJECTED_DISCARD = "REJECTED_DISCARD";
    private static final String DISCARD = "DISCARD";

    private final MongoTemplate mongoTemplate;
    private final ApprovalInstanceService approvalInstanceService;

    @Override
    public ApprovalInstanceSnapshotBackfillResult backfillAll() {
        List<Document> instances = mongoTemplate.findAll(Document.class, APPROVAL_INSTANCE_COLLECTION);
        List<ApprovalInstanceSnapshotBackfillResult.FailedRecord> failures = new ArrayList<>();
        int updated = 0;

        for (Document instance : instances) {
            try {
                if (normalizeLegacyDiscardActions(instance)) {
                    mongoTemplate.save(instance, APPROVAL_INSTANCE_COLLECTION);
                }

                String submissionId = instance.getString("formSubmissionId");
                String collectionName = instance.getString("formSubmissionCollectionName");
                approvalInstanceService.refreshFilterSnapshot(
                        submissionId,
                        collectionName,
                        null
                );
                updated++;
            } catch (Exception e) {
                log.warn("Failed to refresh approval snapshot for instance={} submission={} collection={}",
                        instance.get("_id"), instance.getString("formSubmissionId"), instance.getString("formSubmissionCollectionName"), e);
                failures.add(new ApprovalInstanceSnapshotBackfillResult.FailedRecord(
                        String.valueOf(instance.get("_id")),
                        instance.getString("formSubmissionCollectionName"),
                        instance.getString("formSubmissionId"),
                        e.getMessage()
                ));
            }
        }

        return new ApprovalInstanceSnapshotBackfillResult(
                instances.size(),
                updated,
                failures.size(),
                failures
        );
    }

    private boolean normalizeLegacyDiscardActions(Document instance) {
        boolean updated = false;
        Object actionLogs = instance.containsKey("actionLog") ? instance.get("actionLog") : instance.get("action_log");
        updated |= normalizeActionLogs(actionLogs);
        updated |= normalizeApprovalSteps(instance.get("approvalSteps"));
        return updated;
    }

    private boolean normalizeActionLogs(Object value) {
        if (!(value instanceof List<?> logs)) {
            return false;
        }
        boolean updated = false;
        for (Object log : logs) {
            updated |= normalizeActionHolder(log);
        }
        return updated;
    }

    private boolean normalizeApprovalSteps(Object value) {
        if (!(value instanceof List<?> steps)) {
            return false;
        }
        boolean updated = false;
        for (Object step : steps) {
            if (step instanceof Map<?, ?> stepMap) {
                updated |= normalizeActionHolder(stepMap.get("last_action_record"));
            }
        }
        return updated;
    }

    private boolean normalizeActionHolder(Object value) {
        if (!(value instanceof Map<?, ?> holder)) {
            return false;
        }
        Object action = holder.get("action");
        if (!LEGACY_REJECTED_DISCARD.equals(action)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> mutableHolder = (Map<String, Object>) holder;
        mutableHolder.put("action", DISCARD);
        return true;
    }
}
