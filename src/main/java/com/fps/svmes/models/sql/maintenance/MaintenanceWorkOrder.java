//package com.fps.svmes.models.sql.maintenance;
//
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "work_order", schema = "maintenance_management")
//public class MaintenanceWorkOrder extends Common {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    private String name;
//
//    private String code;
//
//    private String description;
//
//    @Column(name = "start_date")
//    private OffsetDateTime startDate;
//
//    @Column(name = "end_date")
//    private OffsetDateTime endDate;
//
//    @Column(name = "recurrence_type_id")
//    private Integer recurrenceTypeId;
//
//    @Column(name = "image_path")
//    private String imagePath;
//
//    @Column(name = "category_id")
//    private Integer categoryId;
//
//    @Column(name = "priority_id")
//    private Integer priorityId;
//
//    @Column(name = "state_id")
//    private Integer stateId;
//
//    @Column(name = "equipment_id")
//    private Integer equipmentId;
//
//    @Column(name = "work_type_id")
//    private Integer workTypeId;
//
//    @Column(name = "recurrence_uuid")
//    private String recurrenceUuid;
//
//    @Column(name = "due_date")
//    private OffsetDateTime dueDate;
//
//    @Column(name = "estimated_minutes")
//    private BigDecimal estimatedMinutes;
//
//    @Column(name = "file_path")
//    private String filePath;
//
//    @Column(name = "safety_id")
//    private String safetyId;
//
//    @Column(name = "halt_type")
//    private Integer haltType;
//
//    @Column(name = "approved_by_id")
//    private Long approvedById;
//
//    @Column(name = "approved_at")
//    private OffsetDateTime approvedAt;
//
//    @Column(name = "finished_at")
//    private OffsetDateTime finishedAt;
//
//    @Column(name = "food_safety_id")
//    private String foodSafetyId;
//
//    @Column(name = "request_id")
//    private Integer requestId;
//
//    @Column(name = "parent_id")
//    private Integer parentId;
//
//    @Column(name = "component_id")
//    private Long componentId;
//
//    @Column(name = "equipment_group_id")
//    private Integer equipmentGroupId;
//
//    @Column(name = "production_line_id")
//    private Integer productionLineId;
//}
