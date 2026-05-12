package com.fps.svmes.repositories.mongoRepo;

import com.fps.svmes.models.nosql.FormSubmissionLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormSubmissionLockRepository extends MongoRepository<FormSubmissionLock, String> {
    Optional<FormSubmissionLock> findBySubmissionIdAndCollectionName(String submissionId, String collectionName);
}
