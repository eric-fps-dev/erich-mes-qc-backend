package com.fps.svmes.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QcSummaryEmailExportRequest {

    @NotBlank
    @JsonProperty("start_date")
    private String startDate;

    @NotBlank
    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("team_id")
    private Integer teamId;

    @JsonProperty("shift_id")
    private Integer shiftId;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("batch_id")
    private Integer batchId;

    @JsonProperty("form_template_id")
    private Long formTemplateId;

    private String timezone = "UTC";

    @NotEmpty
    private List<@NotBlank @Email String> recipients;

    @Valid
    @NotNull
    private Formats formats;

    private Map<String, String> charts;

    @AssertTrue(message = "At least one export format must be selected")
    public boolean hasAnySelectedFormat() {
        return formats != null && formats.hasAnySelected();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Formats {
        private boolean pdf;
        private boolean excel;
        private boolean ai;

        public boolean hasAnySelected() {
            return pdf || excel || ai;
        }
    }
}
