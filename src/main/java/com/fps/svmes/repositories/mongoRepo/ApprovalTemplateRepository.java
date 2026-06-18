package com.fps.svmes.repositories.mongoRepo;

import com.fps.shared.entity.primary.approval.ApprovalTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalTemplateRepository extends MongoRepository<ApprovalTemplate, String> {
}
