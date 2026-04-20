//package com.fps.svmes.dto.dtos.production;
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
//public class ProductionWorkOrderDTO extends CommonDTO {
//
//    private Integer id;
//
//    private String name;
//
//    private String code;
//
//    private String description;
//
//    @JsonProperty("wo_quantity")
//    private Integer woQuantity;
//
//    @JsonProperty("wo_deadline")
//    private OffsetDateTime woDeadline;
//
//    @JsonProperty("authorized_by_id")
//    private Integer authorizedById;
//
//    @JsonProperty("authorizer_signature")
//    private String authorizerSignature;
//
//    @JsonProperty("authorized_date")
//    private OffsetDateTime authorizedDate;
//
//    private Boolean authorized;
//
//    @JsonProperty("schedule_type")
//    private String scheduleType;
//
//    @JsonProperty("unscheduled_quantity")
//    private Integer unscheduledQuantity;
//
//    @JsonProperty("estimated_production_time")
//    private BigDecimal estimatedProductionTime;
//
//    @JsonProperty("quantity_uom_id")
//    private Integer quantityUomId;
//
//    @JsonProperty("time_uom_id")
//    private Integer timeUomId;
//
//    @JsonProperty("product_id")
//    private Integer productId;
//
//    @JsonProperty("actual_finished_at")
//    private OffsetDateTime actualFinishedAt;
//
//    @JsonProperty("delivered_at")
//    private OffsetDateTime deliveredAt;
//
//    @JsonProperty("actual_quantity")
//    private Integer actualQuantity;
//
//    @JsonProperty("approved_at")
//    private OffsetDateTime approvedAt;
//
//    @JsonProperty("approved_by_id")
//    private Integer approvedById;
//
//    private Boolean bypass;
//
//    @JsonProperty("priority_id")
//    private Integer priorityId;
//
//    @JsonProperty("state_id")
//    private Integer stateId;
//
//    @JsonProperty("start_date")
//    private OffsetDateTime startDate;
//
//    @JsonProperty("recurrence_uuid")
//    private String recurrenceUuid;
//
//    @JsonProperty("image_path")
//    private String imagePath;
//
//    @JsonProperty("file_path")
//    private String filePath;
//
//    @JsonProperty("end_date")
//    private OffsetDateTime endDate;
//
//    @JsonProperty("bypass_list")
//    private String bypassList;
//
//    @JsonProperty("production_line")
//    private String productionLine;
//}
