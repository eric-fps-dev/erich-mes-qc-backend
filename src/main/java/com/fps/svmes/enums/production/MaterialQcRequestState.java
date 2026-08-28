package com.fps.svmes.enums.production;

public enum MaterialQcRequestState {
    DRAFT("draft"),
    PENDING("pending"),
    PASSED("passed"),
    FAILED("failed");

    private final String dbValue;

    MaterialQcRequestState(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean matches(String rawValue) {
        return dbValue.equals(rawValue);
    }
}
