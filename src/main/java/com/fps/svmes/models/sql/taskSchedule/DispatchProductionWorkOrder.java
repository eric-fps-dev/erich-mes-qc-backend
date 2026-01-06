//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import com.fps.svmes.models.sql.production.ProductionWorkOrder;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "dispatch_production_work_order", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchProductionWorkOrder extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dispatch_id", nullable = false)
//    private Dispatch dispatch;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "production_work_order_id", nullable = false)
//    private ProductionWorkOrder productionWorkOrder;
//}
//
//
//
