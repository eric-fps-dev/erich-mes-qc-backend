//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fps.svmes.models.sql.Common;
//import com.fps.svmes.models.sql.production.RawMaterial;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "dispatch_raw_material", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchRawMaterial extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dispatch_id", nullable = false)
//    private Dispatch dispatch;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "raw_material_id", nullable = false)
//    private RawMaterial rawMaterial;
//
//}
//
//
