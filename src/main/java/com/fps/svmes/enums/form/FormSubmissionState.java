package com.fps.svmes.enums.form;

import java.util.Arrays;

public enum FormSubmissionState {
    SUBMITTED("submitted"),
    PENDING_REVISION("pending_revision"),
    UNDER_REVIEW("under_review");

    private final String dbValue;

    FormSubmissionState(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean matches(String rawValue) {
        return dbValue.equals(rawValue);
    }

    public static FormSubmissionState fromValue(String rawValue) {
        return Arrays.stream(values())
                .filter(state -> state.matches(rawValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown form submission state: " + rawValue));
    }
}
