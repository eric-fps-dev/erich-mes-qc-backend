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
//public class EquipmentDTO extends CommonDTO {
//
//    private Short id; // Matches int2 in PostgreSQL
//
//    private String code;
//
//    private String name;
//
//    private String description;
//
//    private String spec;
//
//    private String plc;
//
//    private BigDecimal power; // Matches numeric in PostgreSQL
//
//    @JsonProperty("equipment_class_id")
//    private Integer equipmentClassId;
//
//    @JsonProperty("serial_number")
//    private String serialNumber;
//
//    @JsonProperty("installation_date")
//    private OffsetDateTime installationDate;
//
//    @JsonProperty("availability_status")
//    private Short availabilityStatus; // Matches int2 in PostgreSQL
//
//    @JsonProperty("production_line_id")
//    private Short productionLineId; // Matches int2 in PostgreSQL
//
//    @JsonProperty("image_path")
//    private String imagePath;
//
//    @JsonProperty("equipment_running_state_id")
//    private Short equipmentRunningStateId; // Matches int2 in PostgreSQL
//
//    @JsonProperty("vendor_id")
//    private Short vendorId; // Matches int2 in PostgreSQL
//
//    @JsonProperty("sequence_order")
//    private Short sequenceOrder; // Matches int2 in PostgreSQL
//
//    @JsonProperty("equipment_group_id")
//    private Integer equipmentGroupId;
//
//    @JsonProperty("tree_id")
//    private String treeId; // Matches varchar in PostgreSQL
//
//    @JsonProperty("location_id")
//    private Integer locationId;
//
//    @JsonProperty("file_path")
//    private String filePath;
//}
