package com.fps.svmes.enums.form;

/**
 * Distinguishes which approval model owns a given form submission.
 *
 * LEGACY: embedded approval_info + qc_approval_assignment table
 * V2:     dedicated ApprovalInstance document + MES approval templates
 *
 * New submissions always use V2. Legacy records either have no approvalModel field
 * or were created before the V2 migration.
 */
public enum ApprovalModel {
    LEGACY, V2;

    public static final String DOCUMENT_FIELD = "approvalModel";
    public static final String V2_VALUE = "v2";
}
