//package com.fps.svmes.dto.dtos.dispatch;
//
//import java.time.OffsetDateTime;
//import java.util.List;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import com.fps.svmes.dto.dtos.user.UserDTO;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//@EqualsAndHashCode(callSuper = true)
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@Data
//public class DispatchDTO extends CommonDTO {
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("type")
//    private String type;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("description")
//    private String description;
//
//    @JsonProperty("state")
//    private Short state;
//
//    @JsonProperty("start_time")
//    private OffsetDateTime startTime;
//
//    @JsonProperty("end_time")
//    private OffsetDateTime endTime;
//
//    @JsonProperty("cron_expression")
//    private String cronExpression;
//
//    @JsonProperty("dispatch_limit")
//    private Integer dispatchLimit;
//
//    @JsonProperty("custom_time")
//    private OffsetDateTime customTime;
//
//    @JsonProperty("executed_count")
//    private Integer executedCount;
//
//    @JsonProperty("due_date_offset_minute")
//    private Integer dueDateOffsetMinute;
//
//    @JsonProperty("user_ids")
//    private List<Integer> userIds;
//
//    @JsonProperty("form_ids")
//    private List<String> formIds;
//
//    @JsonProperty("product_ids")
//    private List<Integer> productIds;
//
//    @JsonProperty("raw_material_ids")
//    private List<Integer> rawMaterialIds;
//
//    @JsonProperty("production_work_order_ids")
//    private List<Integer> productionWorkOrderIds;
//
//    @JsonProperty("equipment_ids")
//    private List<Short> equipmentIds;
//
//    @JsonProperty("maintenance_work_order_ids")
//    private List<Integer> maintenanceWorkOrderIds;
//
//    @JsonProperty("sampling_location_ids")
//    private List<Long> samplingLocationIds;
//
//    @JsonProperty("instrument_ids")
//    private List<Long> instrumentIds;
//
//    @JsonProperty("test_subject_ids")
//    private List<Long> testSubjectIds;
//}
