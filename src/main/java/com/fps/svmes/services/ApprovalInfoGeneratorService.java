package com.fps.svmes.services;

import java.util.List;
import java.util.Map;

public interface ApprovalInfoGeneratorService {
    List<Map<String, Object>> generateApprovalInfo(String approvalType, Long createdBy);
}

