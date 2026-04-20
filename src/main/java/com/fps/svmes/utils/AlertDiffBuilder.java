package com.fps.svmes.utils;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;

import java.util.*;

/**
 * Utility class to build a diff map between two versions of an AlertRecordDTO.
 * Used for logging changes into qc_alert_record_log.
 */
public class AlertDiffBuilder {

    public static Map<String, List<String>> buildDiff(AlertRecordDTO oldDto, AlertRecordDTO newDto) {
        Map<String, List<String>> diff = new HashMap<>();

        if (!Objects.equals(oldDto.getRpn(), newDto.getRpn())) {
            diff.put("rpn", Arrays.asList(
                    String.valueOf(oldDto.getRpn()),
                    String.valueOf(newDto.getRpn())
            ));
        }

        if (!Objects.equals(oldDto.getAlertStatus(), newDto.getAlertStatus())) {
            diff.put("alert_status", Arrays.asList(
                    statusText(oldDto.getAlertStatus()),
                    statusText(newDto.getAlertStatus())
            ));
        }

        // Future-proof: add more fields here when editable in frontend
        // e.g. inspection_value, inspector_id, etc.

        return diff;
    }

    private static String statusText(Integer status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case 0 -> "Processing";
            case 1 -> "Closed";
            default -> "Other";
        };
    }
}
