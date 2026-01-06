//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import java.util.List;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "qc_order", schema = "quality_management")
//public class QcOrder extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @Column(name = "name")
//    private String name;
//
//    @Column(name = "state")
//    private Short state = QcOrderState.Active.getState();
//
//    @Column(name = "description")
//    private String description;
//
//    @OneToMany(mappedBy = "qcOrder", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<QcOrderDispatch> qcOrderDispatches;
//}
