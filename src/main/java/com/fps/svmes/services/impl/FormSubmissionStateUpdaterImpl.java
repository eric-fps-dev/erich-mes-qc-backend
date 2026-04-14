package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.enums.form.FormSubmissionState;
import com.fps.svmes.services.FormSubmissionStateUpdater;
import com.fps.svmes.services.QcFormTemplateService;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FormSubmissionStateUpdaterImpl implements FormSubmissionStateUpdater {
    private final MongoTemplate mongoTemplate;
    private final QcFormTemplateService qcFormTemplateService;

    @Override
    public void updateFormSubmissionState(String submissionId, String collectionName, FormSubmissionState state, Long updatedBy) {
        Update update = new Update()
                .set("state", state.dbValue())
                .set("updated_at", new Date());
        UpdateResult result = mongoTemplate.updateFirst(submissionIdQuery(submissionId), update, collectionName);
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Form submission state update failed because submission was not found: " + submissionId);
        }
    }

    @Override
    public Document getLatestFormSubmission(String submissionId, String collectionName) {
        // Approval-instance actions should validate against the exact submission currently
        // referenced by the approval instance, not the newest sibling in the version group.
        return findSubmission(submissionId, collectionName);
    }

    @Override
    public Document getFormTemplateSnapshot(String formTemplateId) {
        QcFormTemplateDTO template = qcFormTemplateService.getTemplateById(Long.parseLong(formTemplateId));
        if (template == null) {
            return null;
        }
        return new Document()
                .append("id", template.getId())
                .append("name", template.getName())
                .append("form_template_json", template.getFormTemplateJson())
                .append("approval_type", template.getApprovalType())
                .append("approval_template_id", template.getApprovalTemplateId())
                .append("has_edit_history", template.getHasEditHistory())
                .append("created_at", offsetDateTimeToDate(template.getCreatedAt()))
                .append("created_by", template.getCreatedBy())
                .append("updated_at", offsetDateTimeToDate(template.getUpdatedAt()))
                .append("updated_by", template.getUpdatedBy())
                .append("status", template.getStatus());
    }

    private Document findSubmission(String submissionId, String collectionName) {
        if (!ObjectId.isValid(submissionId)) {
            throw new IllegalArgumentException("Invalid submissionId format: " + submissionId);
        }
        Document document = mongoTemplate.findOne(submissionIdQuery(submissionId), Document.class, collectionName);
        if (document == null) {
            throw new IllegalArgumentException("Submission not found: " + submissionId);
        }
        return document;
    }

    private Query submissionIdQuery(String submissionId) {
        return new Query(Criteria.where("_id").is(new ObjectId(submissionId)));
    }

    private Date offsetDateTimeToDate(OffsetDateTime value) {
        return value == null ? null : Date.from(value.toInstant());
    }
}
