//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import com.fps.svmes.models.sql.maintenance.Equipment;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "dispatch_equipment", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchEquipment extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dispatch_id", nullable = false)
//    private Dispatch dispatch;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "equipment_id", nullable = false)
//    private Equipment equipment;
//}
//
//
//
//
