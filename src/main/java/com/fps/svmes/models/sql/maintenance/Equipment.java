//package com.fps.svmes.models.sql.maintenance;
//
//import com.fps.svmes.models.sql.Common;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import jakarta.persistence.*;
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "equipment", schema = "maintenance_management")
//public class Equipment extends Common {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
//    @Column(name = "equipment_class_id")
//    private Integer equipmentClassId;
//
//    @Column(name = "serial_number")
//    private String serialNumber;
//
//    @Column(name = "installation_date")
//    private OffsetDateTime installationDate;
//
//    @Column(name = "availability_status")
//    private Short availabilityStatus; // Matches int2 in PostgreSQL
//
//    @Column(name = "production_line_id")
//    private Short productionLineId; // Matches int2 in PostgreSQL
//
//    @Column(name = "image_path")
//    private String imagePath;
//
//    @Column(name = "equipment_running_state_id")
//    private Short equipmentRunningStateId; // Matches int2 in PostgreSQL
//
//    @Column(name = "vendor_id")
//    private Short vendorId; // Matches int2 in PostgreSQL
//
//    @Column(name = "sequence_order")
//    private Short sequenceOrder; // Matches int2 in PostgreSQL
//
//    @Column(name = "equipment_group_id")
//    private Integer equipmentGroupId;
//
//    @Column(name = "tree_id")
//    private String treeId; // Matches varchar in PostgreSQL
//
//    @Column(name = "location_id")
//    private Integer locationId;
//
//    @Column(name = "file_path")
//    private String filePath;
//}
