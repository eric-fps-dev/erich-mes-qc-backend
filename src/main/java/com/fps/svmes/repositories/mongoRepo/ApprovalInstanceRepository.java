package com.fps.svmes.repositories.mongoRepo;

import com.fps.svmes.models.nosql.approval.ApprovalInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalInstanceRepository extends MongoRepository<ApprovalInstance, String> {
    Optional<ApprovalInstance> findByFormSubmissionIdAndFormSubmissionCollectionName(
            String formSubmissionId,
            String formSubmissionCollectionName
    );

    Optional<ApprovalInstance> findByFormSubmissionId(String formSubmissionId);

    List<ApprovalInstance> findByFormSubmissionCollectionNameAndFormSubmissionIdIn(
            String formSubmissionCollectionName,
            Collection<String> formSubmissionIds
    );

    List<ApprovalInstance> findByFormSubmissionCollectionName(String formSubmissionCollectionName);

    List<ApprovalInstance> findByFormSubmissionCollectionNameAndApprovalTemplateId(
            String formSubmissionCollectionName,
            String approvalTemplateId
    );
}
