package com.fps.svmes.enums.approval;

public enum ApprovalProcessStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    COMPLETE("complete");

    private final String dbValue;

    ApprovalProcessStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }
}
