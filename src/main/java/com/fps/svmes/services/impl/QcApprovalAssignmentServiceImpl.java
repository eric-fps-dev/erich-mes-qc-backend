package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcApprovalAssignmentDTO;
import com.fps.svmes.models.sql.qcForm.QcApprovalAssignment;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcApprovalAssignmentRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.QcApprovalAssignmentService;
import com.fps.svmes.utils.MongoFormTemplateUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QcApprovalAssignmentServiceImpl implements QcApprovalAssignmentService {

    private final ModelMapper modelMapper;
    private final QcApprovalAssignmentRepository qcApprovalAssignmentRepository;
    private final MongoClient mongoClient;

    @Autowired
    MongoFormTemplateUtils mongoUtils;

    @Autowired
    private final UserRepository userRepository;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabaseName;

    @Override
    public void insertIfNotExists(QcApprovalAssignmentDTO dto) {
        Optional<QcApprovalAssignment> existing = qcApprovalAssignmentRepository.findBySubmissionId(dto.getSubmissionId());
        if (existing.isEmpty()) {
            QcApprovalAssignment entity = modelMapper.map(dto, QcApprovalAssignment.class);
            qcApprovalAssignmentRepository.save(entity);
        }
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getAllAssignments(Pageable pageable) {
        return qcApprovalAssignmentRepository.findAll(pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

    @Override
    public Page<QcApprovalAssignmentDTO> getFilteredAssignments(
            String state,
            String approvalType,
            String templateName,
            String startDate,
            String endDate,
            Pageable pageable) {

        Specification<QcApprovalAssignment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(state)) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            if (StringUtils.hasText(approvalType)) {
                predicates.add(cb.equal(root.get("approvalType"), approvalType));
            }
            if (StringUtils.hasText(templateName)) {
                predicates.add(cb.like(root.get("qcFormTemplateName"), "%" + templateName + "%"));
            }
            if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
                predicates.add(cb.between(root.get("createdAt"),
                        LocalDate.parse(startDate).atStartOfDay(),
                        LocalDate.parse(endDate).atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return qcApprovalAssignmentRepository.findAll(spec, pageable)
                .map(entity -> modelMapper.map(entity, QcApprovalAssignmentDTO.class));
    }

    @Override
    public List<Document> getVersionHistory(String submissionId, String collectionName) {
        // Step 1: Connect to the collection
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        // Step 2: Find the initial document by submission ID
        Document initial = collection.find(Filters.eq("_id", new org.bson.types.ObjectId(submissionId))).first();
        if (initial == null) throw new IllegalArgumentException("Submission not found");

        // Step 3: Extract version_group_id
        String versionGroupId = initial.getString("version_group_id");

        // Step 4: Return all versions in descending order
        List<Document> rawVersions;
        if (versionGroupId == null || versionGroupId.isEmpty()) {
            // ❗ 无 version_group_id，返回当前记录
            rawVersions = List.of(initial);
        } else {
            // ✅ 有 version_group_id，返回该组全部版本，按 version 降序
            rawVersions = collection.find(Filters.eq("version_group_id", versionGroupId))
                    .sort(new Document("version", -1))
                    .into(new ArrayList<>());
        }

        // 获取 formTemplateId（通过 collectionName 拆解出来）
        String[] parts = collectionName.split("_");
        if (parts.length < 3) throw new IllegalArgumentException("Invalid collection name format");
        Long formTemplateId = Long.parseLong(parts[2]);

        return rawVersions.stream()
                .map(doc -> mongoUtils.formatRecord(
                        doc,
                        mongoUtils.getOptionItemsKeyValueMapping(formTemplateId),
                        mongoUtils.getFormTemplateKeyValueMapping(formTemplateId)
                ))
                .collect(Collectors.toList());

    }

    @Transactional
    @Override
    public void approveAction(String submissionId, String collectionName, Integer approverId, String comment, boolean suggestRetest, String eSignatureBase64) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        // Step 1: Find the document
        Document doc = collection.find(Filters.eq("_id", new ObjectId(submissionId))).first();
        if (doc == null) {
            throw new RuntimeException("❌ Document not found for submissionId: " + submissionId);
        }

        List<Document> approvalInfo = (List<Document>) doc.get("approval_info");
        if (approvalInfo == null) {
            throw new RuntimeException("❌ approval_info not found in document.");
        }

        // Step 2: Find the current approval step
        int currentIndex = -1;
        for (int i = 0; i < approvalInfo.size(); i++) {
            Document step = approvalInfo.get(i);
            if ("pending".equals(step.getString("status"))) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new RuntimeException("❌ No pending approval step found");
        }

        // Step 3: Update current step
        Document currentStep = approvalInfo.get(currentIndex);
        currentStep.put("comments", comment);
        currentStep.put("user_id", approverId);
        currentStep.put("user_name", userRepository.findNameById(approverId));
        currentStep.put("suggest_retest", suggestRetest);
        currentStep.put("e-signature", eSignatureBase64);
        currentStep.put("timestamp", new Date());
        currentStep.put("status", "completed");

        // Step 4: Advance next step
        Document nextStep;
        String nextRole;

        if (currentIndex + 1 < approvalInfo.size()) {
            nextStep = approvalInfo.get(currentIndex + 1);
            nextRole = nextStep.getString("role");

            if ("archive".equals(nextRole)) {
                // auto-complete archive step
                nextStep.put("status", "completed");
                nextStep.put("timestamp", new Date());
            } else {
                // next human step becomes pending
                nextStep.put("status", "pending");
            }
        } else {
            nextRole = null;
        }

        // Step 5: Update back to MongoDB
        collection.updateOne(
                Filters.eq("_id", new ObjectId(submissionId)),
                new Document("$set", new Document("approval_info", approvalInfo)
                        .append("approver_updated_at", new Date()))
        );

        // Step 6: Update PostgreSQL snapshot state
        qcApprovalAssignmentRepository.findBySubmissionId(submissionId).ifPresent(snapshot -> {
            String updatedState = computeSnapshotStateFromNextRole(nextRole);

            snapshot.setState(updatedState);
            snapshot.setUpdatedAt(OffsetDateTime.now());
            qcApprovalAssignmentRepository.save(snapshot);
        });
    }

    private String computeSnapshotStateFromNextRole(String nextRole) {
        if (nextRole == null || "archive".equals(nextRole)) {
            return "fully_approved";
        }
        if ("leader".equals(nextRole)) {
            return "pending_leader";
        }
        if ("supervisor".equals(nextRole)) {
            return "pending_supervisor";
        }
        // Fallback for unexpected roles
        return "fully_approved";
    }

    @Override
    public List<Document> getApprovalInfo(String submissionId, String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document doc = collection.find(Filters.eq("_id", new ObjectId(submissionId))).first();
        if (doc == null) {
            throw new RuntimeException("Document not found for submissionId: " + submissionId);
        }

        List<Document> approvalInfo = (List<Document>) doc.get("approval_info");
        if (approvalInfo == null) {
            throw new RuntimeException("approval_info not found in document.");
        }

        return approvalInfo;
    }

    @Override
    public void updateSubmissionId(String oldSubmissionId, String newSubmissionId) {
        Optional<QcApprovalAssignment> assignmentOpt = qcApprovalAssignmentRepository.findBySubmissionId(oldSubmissionId);
        assignmentOpt.ifPresent(assignment -> {
            assignment.setSubmissionId(newSubmissionId);
            assignment.setUpdatedAt(OffsetDateTime.now());
            qcApprovalAssignmentRepository.save(assignment);
        });
    }


}
