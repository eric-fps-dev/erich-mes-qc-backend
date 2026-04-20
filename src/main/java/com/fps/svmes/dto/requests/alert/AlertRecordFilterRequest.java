package com.fps.svmes.dto.requests.alert;

import lombok.Data;
import java.util.Map;

@Data
public class AlertRecordFilterRequest {
    private int page;
    private int size;
    private Map<String, String> filters;
    private SortSettings sort;
    private Integer status = 1;

    @Data
    public static class SortSettings {
        private String prop;
        private String order; // "ascending" or "descending"
    }
}
