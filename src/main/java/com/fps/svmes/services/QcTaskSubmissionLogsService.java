package com.fps.svmes.services;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;

import org.bson.Document;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface QcTaskSubmissionLogsService {
    QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto);
    List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId);

    Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy, Optional<String> inputCollectionName);

    List<Document> getDocumentsByQcFormTemplateIdAndCreatedBy(Long qcFormTemplateId, Integer createdBy);

    byte[] exportDocumentsToExcel(List<Document> documents);

    byte[] exportDocumentToPdf(Document document);

    void deleteSubmissionLog(String submissionId, String collectionName);

    Document getRawDocumentBySubmissionId(String submissionId, String collectionName);

    List<Map<String, Object>> getFormTemplateFieldList(Long formId);

}