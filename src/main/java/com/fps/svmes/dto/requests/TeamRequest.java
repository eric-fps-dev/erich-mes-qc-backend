//package com.fps.svmes.dto.requests;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.OffsetTime;
//import java.time.OffsetDateTime;
//import java.util.List;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class TeamRequest {
//
//    @JsonProperty("id")
//    @Schema(description = "Team ID (only for updates)", example = "1")
//    private Integer id;
//
//    @JsonProperty("name")
//    @NotNull(message = "Team name cannot be null")
//    @Schema(description = "Name of the team", example = "Morning Team")
//    private String name;
//
//    @JsonProperty("type")
//    @NotNull(message = "Team type cannot be null")
//    @Schema(description = "Type of the team", example = "Day")
//    private String type;
//
//    @JsonProperty("leader_id") // 与前端字段名称一致
//    @Schema(description = "User ID of the team leader", example = "14")
//    private Integer leaderId;
//
//    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "08:00:00+02:00")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
//    @JsonProperty("start_time")
//    private OffsetTime startTime;
//
//    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "16:00:00+02:00")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
//    @JsonProperty("end_time")
//    private OffsetTime endTime;
//
//    @JsonProperty("description")
//    @Schema(description = "Description of the team", example = "This is a regular day team")
//    private String description;
//
//    @JsonProperty("status")
//    @Schema(description = "Status of the team", example = "1")
//    private Integer status;
//
//    @JsonProperty("created_by")
//    @Schema(description = "User ID that creates this team", example = "14")
//    private Integer createdBy;
//
//    @JsonProperty("updated_by")
//    @Schema(description = "User ID that updates this team", example = "14")
//    private Integer updatedBy;
//
//    @JsonProperty("created_at")
//    @Schema(description = "Creation timestamp of the team", example = "12:00:00+00:00")
//    private OffsetDateTime createdAt;
//
//    @JsonProperty("updated_at")
//    @Schema(description = "Last update timestamp of the team", example = "12:00:00+00:00")
//    private OffsetDateTime updatedAt;
//
//    @JsonProperty("parent_id")
//    @Schema(description = "Parent team id", example = "1")
//    private Integer parentId;
//
//    @JsonProperty("member_ids")
//    @Schema(description = "List of integer of member ids assigned to this team")
//    private List<Integer> memberIds;
//
//    @JsonProperty("form_ids")
//    @Schema(description = "List of string of form ids assigned to this team")
//    private List<String> formIds;
//}
