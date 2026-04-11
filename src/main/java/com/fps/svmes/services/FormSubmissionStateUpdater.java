package com.fps.svmes.services;

import com.fps.svmes.enums.form.FormSubmissionState;
import org.bson.Document;

public interface FormSubmissionStateUpdater {
    void updateFormSubmissionState(String submissionId, String collectionName, FormSubmissionState state, Long updatedBy);

    Document getLatestFormSubmission(String submissionId, String collectionName);

    Document getFormTemplateSnapshot(String formTemplateId);
}
