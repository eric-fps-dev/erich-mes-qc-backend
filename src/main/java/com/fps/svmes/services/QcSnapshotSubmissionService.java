package com.fps.svmes.services;

import java.util.List;

public interface QcSnapshotSubmissionService {
    void updateSubmissionId(String oldSubmissionId, String newSubmissionId);
    void deleteBySubmissionId(String submissionId);
    void deleteBySubmissionIds(List<String> submissionIds);
}
