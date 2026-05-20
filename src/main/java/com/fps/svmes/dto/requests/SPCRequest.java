package com.fps.svmes.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SPCRequest {

    @NotNull(message = "formTemplateId is required")
    @Positive
    @Schema(description = "Form Template ID", example = "604")
    private Long formTemplateId;

    @NotNull(message = "startDateTime is required")

    @Schema(description = "Start Date Time", example = "2026-01-01T00:00:00.000-08:00")
    private OffsetDateTime startDateTime;

    @NotNull(message = "endDateTime is required")
    @Schema(description = "End Date Time", example = "2026-01-08T00:00:00.000-08:00")
    private OffsetDateTime endDateTime;

    @Schema(description = "List of Fields", example = "['field_1', 'field_2']")
    private ArrayList<String> fields;
}
