package com.fps.svmes.services.impl;

import com.fps.svmes.enums.approval.ApprovalProcessStatus;
import com.fps.svmes.enums.approval.ApprovalStepState;
import com.fps.svmes.models.nosql.approval.ApprovalInstanceStep;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class ApprovalProcessStatusResolver {

    private ApprovalProcessStatusResolver() {
    }

    static String deriveDbValue(List<ApprovalInstanceStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return ApprovalProcessStatus.PENDING.dbValue();
        }
        return deriveDbValueFromStates(steps.stream()
                .map(ApprovalInstanceStep::getStepState)
                .toList());
    }

    static String deriveDbValueFromStates(Collection<ApprovalStepState> stepStates) {
        List<ApprovalStepState> normalizedStates = stepStates == null
                ? List.of()
                : stepStates.stream().filter(Objects::nonNull).toList();
        if (normalizedStates.isEmpty()) {
            return ApprovalProcessStatus.PENDING.dbValue();
        }
        if (normalizedStates.stream().allMatch(stepState -> stepState == ApprovalStepState.PENDING)) {
            return ApprovalProcessStatus.PENDING.dbValue();
        }
        if (normalizedStates.stream().allMatch(ApprovalProcessStatusResolver::isResolvedStepState)) {
            return ApprovalProcessStatus.COMPLETE.dbValue();
        }
        return ApprovalProcessStatus.IN_PROGRESS.dbValue();
    }

    private static boolean isResolvedStepState(ApprovalStepState stepState) {
        return stepState == ApprovalStepState.APPROVED
                || stepState == ApprovalStepState.FORWARDED
                || stepState == ApprovalStepState.VOIDED;
    }
}
