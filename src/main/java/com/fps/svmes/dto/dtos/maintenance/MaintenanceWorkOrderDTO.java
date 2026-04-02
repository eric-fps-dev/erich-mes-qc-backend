//package com.fps.svmes.dto.dtos.maintenance;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//
//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@EqualsAndHashCode(callSuper = true)
//public class MaintenanceWorkOrderDTO extends CommonDTO {
//    private Integer id;
//
//    private String name;
//
//    private String code;
//
//    private String description;
//
//    @JsonProperty("start_date")
//    private OffsetDateTime startDate;
//
//    @JsonProperty("end_date")
//    private OffsetDateTime endDate;
//
//    @JsonProperty("recurrence_type_id")
//    private Integer recurrenceTypeId;
//
//    @JsonProperty("image_path")
//    private String imagePath;
//
//    @JsonProperty("category_id")
//    private Integer categoryId;
//
//    @JsonProperty("priority_id")
//    private Integer priorityId;
//
//    @JsonProperty("state_id")
//    private Integer stateId;
//
//    @JsonProperty("equipment_id")
//    private Integer equipmentId;
//
//    @JsonProperty("work_type_id")
//    private Integer workTypeId;
//
//    @JsonProperty("recurrence_uuid")
//    private String recurrenceUuid;
//
//    @JsonProperty("due_date")
//    private OffsetDateTime dueDate;
//
//    @JsonProperty("estimated_minutes")
//    private BigDecimal estimatedMinutes;
//
//    @JsonProperty("file_path")
//    private String filePath;
//
//    @JsonProperty("safety_id")
//    private String safetyId;
//
//    @JsonProperty("halt_type")
//    private Integer haltType;
//
//    @JsonProperty("approved_by_id")
//    private Long approvedById;
//
//    @JsonProperty("approved_at")
//    private OffsetDateTime approvedAt;
//
//    @JsonProperty("finished_at")
//    private OffsetDateTime finishedAt;
//
//    @JsonProperty("food_safety_id")
//    private String foodSafetyId;
//
//    @JsonProperty("request_id")
//    private Integer requestId;
//
//    @JsonProperty("parent_id")
//    private Integer parentId;
//
//    @JsonProperty("component_id")
//    private Long componentId;
//
//    @JsonProperty("equipment_group_id")
//    private Integer equipmentGroupId;
//
//    @JsonProperty("production_line_id")
//    private Integer productionLineId;
//}
