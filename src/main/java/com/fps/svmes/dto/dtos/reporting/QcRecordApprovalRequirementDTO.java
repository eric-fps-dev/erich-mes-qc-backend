package com.fps.svmes.dto.dtos.reporting;

import java.util.List;

public class QcRecordApprovalRequirementDTO {
    private List<String> approvalRequiredUserIds;
    private List<String> approvalRequiredRoleIds;

    public QcRecordApprovalRequirementDTO() {
    }

    public QcRecordApprovalRequirementDTO(List<String> approvalRequiredUserIds, List<String> approvalRequiredRoleIds) {
        this.approvalRequiredUserIds = approvalRequiredUserIds;
        this.approvalRequiredRoleIds = approvalRequiredRoleIds;
    }

    public List<String> getApprovalRequiredUserIds() {
        return approvalRequiredUserIds;
    }

    public void setApprovalRequiredUserIds(List<String> approvalRequiredUserIds) {
        this.approvalRequiredUserIds = approvalRequiredUserIds;
    }

    public List<String> getApprovalRequiredRoleIds() {
        return approvalRequiredRoleIds;
    }

    public void setApprovalRequiredRoleIds(List<String> approvalRequiredRoleIds) {
        this.approvalRequiredRoleIds = approvalRequiredRoleIds;
    }
}
