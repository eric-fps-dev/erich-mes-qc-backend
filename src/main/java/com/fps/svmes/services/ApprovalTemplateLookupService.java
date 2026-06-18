package com.fps.svmes.services;

import com.fps.shared.entity.primary.approval.ApprovalTemplate;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApprovalTemplateLookupService {
    private final MongoTemplate approvalTemplateMongoTemplate;

    public ApprovalTemplateLookupService(
            MongoClient mongoClient,
            @Value("${qc.approval-template.mongodb.database:${spring.data.mongodb.database}}") String approvalTemplateDatabase
    ) {
        this.approvalTemplateMongoTemplate = new MongoTemplate(mongoClient, approvalTemplateDatabase);
    }

    public ApprovalTemplate findById(String approvalTemplateId) {
        return approvalTemplateMongoTemplate.findById(approvalTemplateId, ApprovalTemplate.class);
    }
}
