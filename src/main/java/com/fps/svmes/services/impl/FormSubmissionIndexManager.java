package com.fps.svmes.services.impl;

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
    private static final Pattern FORM_COLLECTION_PATTERN = Pattern.compile("^form_template_\\d+_\\d{6}$");

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureExistingIndexes() {
        ensureApprovalInstanceIndexes();
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            if (FORM_COLLECTION_PATTERN.matcher(collectionName).matches()) {
                ensureFormSubmissionIndexes(collectionName);
            }
        }
    }

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

    private void ensureApprovalInstanceIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(ApprovalInstance.class);
        indexOps.ensureIndex(new Index().on("formSubmissionCollectionName", Sort.Direction.ASC).on("formSubmissionId", Sort.Direction.ASC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.formSubmissionState", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("approvalTemplateId", Sort.Direction.ASC)
                .on("filterSnapshot.formSubmissionState", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.formTemplateId", Sort.Direction.ASC)
                .on("filterSnapshot.formSubmissionState", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.createdBy", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.relatedInspectorIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.relatedProductIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.relatedBatchIds", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.relatedTeamId", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)
                .on("filterSnapshot.relatedShiftId", Sort.Direction.ASC)
                .on("filterSnapshot.createdAt", Sort.Direction.DESC));
    }
}
