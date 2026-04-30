package com.fps.svmes.enums.approval;

public enum ApprovalAction {
    SUBMITTED_FOR_APPROVAL,
    RECALLED,
    FLOW_EDITED,
    APPROVED,
    AUTO_APPROVED,
    FORWARDED,
    REQUESTED_CORRECTION,
    REJECTED_FULL_RESET,  // Legacy state
    REJECTED_PARTIAL_RESET,  // Legacy state
    REJECTED_DISCARD
}
