//package com.fps.svmes.models.sql.production;
//
//import com.fps.shared.entity.BaseEntity;
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
//@Table(name = "production_workorder_mgnt", schema = "production_management")
//public class ProductionWorkOrder extends BaseEntity {
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
//    @Column(name = "wo_quantity")
//    private Integer woQuantity;
//
//    @Column(name = "wo_deadline")
//    private OffsetDateTime woDeadline;
//
//    @Column(name = "authorized_by_id")
//    private Integer authorizedById;
//
//    @Column(name = "authorizer_signature")
//    private String authorizerSignature;
//
//    @Column(name = "authorized_date")
//    private OffsetDateTime authorizedDate;
//
//    private Boolean authorized;
//
//    @Column(name = "schedule_type")
//    private String scheduleType;
//
//    @Column(name = "unscheduled_quantity")
//    private Integer unscheduledQuantity;
//
//    @Column(name = "estimated_production_time")
//    private BigDecimal estimatedProductionTime;
//
//    @Column(name = "quantity_uom_id")
//    private Integer quantityUomId;
//
//    @Column(name = "time_uom_id")
//    private Integer timeUomId;
//
//    @Column(name = "product_id")
//    private Integer productId;
//
//    @Column(name = "actual_finished_at")
//    private OffsetDateTime actualFinishedAt;
//
//    @Column(name = "delivered_at")
//    private OffsetDateTime deliveredAt;
//
//    @Column(name = "actual_quantity")
//    private Integer actualQuantity;
//
//    @Column(name = "approved_at")
//    private OffsetDateTime approvedAt;
//
//    @Column(name = "approved_by_id")
//    private Integer approvedById;
//
//    private Boolean bypass;
//
//    @Column(name = "priority_id")
//    private Integer priorityId;
//
//    @Column(name = "state_id")
//    private Integer stateId;
//
//    @Column(name = "start_date")
//    private OffsetDateTime startDate;
//
//    @Column(name = "recurrence_uuid")
//    private String recurrenceUuid;
//
//    @Column(name = "image_path")
//    private String imagePath;
//
//    @Column(name = "file_path")
//    private String filePath;
//
//    @Column(name = "end_date")
//    private OffsetDateTime endDate;
//
//    @Column(name = "bypass_list")
//    private String bypassList;
//
//    @Column(name = "production_line")
//    private String productionLine;
//}
