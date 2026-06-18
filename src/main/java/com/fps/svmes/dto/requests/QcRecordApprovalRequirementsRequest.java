package com.fps.svmes.dto.requests;

import java.util.List;

public class QcRecordApprovalRequirementsRequest {
    private List<String> recordIds;

    public List<String> getRecordIds() {
        return recordIds;
    }

    public void setRecordIds(List<String> recordIds) {
        this.recordIds = recordIds;
    }
}
