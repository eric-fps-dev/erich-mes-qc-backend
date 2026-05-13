package com.fps.svmes.services.impl;

import com.fps.svmes.models.nosql.FormSubmissionLock;
import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class FormSubmissionIndexManager {
    /**
     * Dynamic form submission collections follow this naming convention:
     * form_template_{templateId}_{yyyyMM}
     */
    private static final Pattern FORM_COLLECTION_PATTERN = Pattern.compile("^form_template_\\d+_\\d{6}$");

    private final MongoTemplate mongoTemplate;

    /**
     * Runs once after Spring finishes bean initialization.
     Ensure indexes for approval instances and all existing form submission collections at startup.
     New collections created later should also call ensureFormSubmissionIndexes().
     */
    @PostConstruct
    public void ensureExistingIndexes() {
        ensureApprovalInstanceIndexes();
        ensureFormSubmissionLockIndexes();
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            if (FORM_COLLECTION_PATTERN.matcher(collectionName).matches()) {
                ensureFormSubmissionIndexes(collectionName);
            }
        }
    }

    // Indexes support common form submission filters plus latest-first sorting.
    // Keep this aligned with actual query patterns to avoid unnecessary index cost.
    public void ensureFormSubmissionIndexes(String collectionName) {
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        indexOps.ensureIndex(new Index().on("state", Sort.Direction.ASC).on("created_at", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("created_by", Sort.Direction.ASC).on("created_at", Sort.Direction.DESC));
        for (String fieldName : List.of(
                "related_inspector_ids",
                "related_product_ids",
                "related_batch_ids",
                "related_team_id",
                "related_shift_id"
        )) {
            indexOps.ensureIndex(new Index().on(fieldName, Sort.Direction.ASC).on("created_at", Sort.Direction.DESC));
        }
    }

    // ApprovalInstance indexes support:
    // 1. form submission linkage lookup
    // 2. approval list filters using filterSnapshot fields
    private void ensureApprovalInstanceIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(ApprovalInstance.class);
        dropApprovalInstanceLegacyIndexes(indexOps);
        indexOps.ensureIndex(new Index().on("formSubmissionCollectionName", Sort.Direction.ASC).on("formSubmissionId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("formSubmissionCollectionName", Sort.Direction.ASC)
                .on("approvalTemplateId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("approvalTemplateId", Sort.Direction.ASC)
                .on("filterSnapshot.formSubmissionState", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.formTemplateId", Sort.Direction.ASC)
                .on("filterSnapshot.formSubmissionState", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.createdBy", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.relatedInspectorIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.relatedProductIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.relatedBatchIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.relatedTeamId", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.relatedShiftId", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("filterSnapshot.isAlarmTriggered", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("approvalSteps.stepState", Sort.Direction.ASC)
                .on("approvalSteps.requiredUserId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("approvalSteps.stepState", Sort.Direction.ASC)
                .on("approvalSteps.requiredRoleId", Sort.Direction.ASC));
    }

    private void dropApprovalInstanceLegacyIndexes(IndexOperations indexOps) {
        for (String indexName : List.of(
                "formSubmissionCollectionName_1_approvalTemplateId_1_state_1",
                "status_1_approvalTemplateId_1_filterSnapshot.formSubmissionState_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.formTemplateId_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.createdBy_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.relatedInspectorIds_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.relatedProductIds_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.relatedBatchIds_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.relatedTeamId_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.relatedShiftId_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.formSubmissionState_1_filterSnapshot.createdAt_-1",
                "status_1_filterSnapshot.formTemplateId_1_filterSnapshot.formSubmissionState_1_filterSnapshot.createdAt_-1"
        )) {
            try {
                indexOps.dropIndex(indexName);
                log.info("Dropped legacy approval-instance index {}", indexName);
            } catch (RuntimeException e) {
                log.debug("Approval-instance index {} not dropped: {}", indexName, e.getMessage());
            }
        }
    }

    private void ensureFormSubmissionLockIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(FormSubmissionLock.class);
        indexOps.ensureIndex(new Index()
                .on("submissionId", Sort.Direction.ASC)
                .on("collectionName", Sort.Direction.ASC)
                .unique()
                .named("ux_submission_collection"));
        indexOps.ensureIndex(new Index()
                .on("expiresAt", Sort.Direction.ASC)
                .expire(0)
                .named("idx_submission_lock_expires_at"));
    }
}
