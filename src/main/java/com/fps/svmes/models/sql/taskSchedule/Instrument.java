//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import com.fps.shared.entity.BaseEntity;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "instrument", schema = "quality_management")
//public class Instrument extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @Column(name = "type")
//    private String type;
//
//    @Column(name = "name")
//    private String name;
//    @Column(name = "description")
//    private String description;
//
//    @Column(name = "manufacturer")
//    private String manufacturer;
//
//    @Column(name = "model_number")
//    private String modelNumber;
//
//}
