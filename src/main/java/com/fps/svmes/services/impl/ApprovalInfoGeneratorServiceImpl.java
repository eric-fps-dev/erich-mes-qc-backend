package com.fps.svmes.services.impl;

import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.ApprovalInfoGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ApprovalInfoGeneratorServiceImpl implements ApprovalInfoGeneratorService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Map<String, Object>> generateApprovalInfo(String approvalType, Long createdBy) {
        List<Map<String, Object>> flow = new ArrayList<>();

        // Always add the submitter with current timestamp
        flow.add(createNode("submitter", createdBy, "QC Worker", "completed", Date.from(Instant.now())));

        if ("flow_2".equals(approvalType)) {
            flow.add(createNode("leader", null, "Leader Sign", "pending", null));
        } else if ("flow_3".equals(approvalType)) {
            flow.add(createNode("supervisor", null, "Supervisor Sign", "pending", null));
        } else if ("flow_4".equals(approvalType)) {
            flow.add(createNode("leader", null, "Leader Sign", "pending", null));
            flow.add(createNode("supervisor", null, "Supervisor Sign", "not_started", null));
        }

        // Final archive node
        flow.add(createNode("archive", null, "Archived", "not_started", null));
        return flow;
    }

    private Map<String, Object> createNode(String role, Long userId, String label, String status, Date timestamp) {
        Map<String, Object> node = new HashMap<>();
        node.put("role", role);
        node.put("user_id", userId);
        if (userId != null) {
            node.put("user_name", userRepository.findNameById(Math.toIntExact(userId)));
        }
        node.put("label", label);
        node.put("status", status);
        node.put("timestamp", timestamp);
        node.put("comments", null);
        node.put("suggest_retest", null);

        if (!"submitter".equals(role)) {
            node.put("e-signature", null);
        }

        return node;
    }

}
