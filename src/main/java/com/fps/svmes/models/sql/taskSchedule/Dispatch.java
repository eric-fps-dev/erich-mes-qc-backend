//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import java.time.OffsetDateTime;
//import java.util.List;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "dispatch", schema = "quality_management")
//public class Dispatch extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @Column(name = "type", nullable = false)
//    private String type;
//
//    @Column(name = "name")
//    private String name;
//
//    @Column(name = "description")
//    private String description;
//
//    @Column(name = "state", nullable = false)
//    private Short state = 1;
//
//    @Column(name = "start_time", nullable = false)
//    private OffsetDateTime startTime; // For regular type dispatch only
//
//    @Column(name = "end_time")
//    private OffsetDateTime endTime; // For regular type dispatch only
//
//    @Column(name = "cron_expression")
//    private String cronExpression; // For regular type dispatch only
//
//    @Column(name = "dispatch_limit")
//    private Integer dispatchLimit; // For regular type dispatch only
//
//    @Column(name = "custom_time")
//    private OffsetDateTime customTime; // For custom type dispatch only
//
//    @Column(name = "executed_count")
//    private Integer executedCount = 0;
//
//    @Column(name = "due_date_offset_minute")
//    private Integer dueDateOffsetMinute; // Total offset in minutes, default to 1 hour
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchUser> dispatchUsers;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchForm> dispatchForms;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchProduct> dispatchProducts;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchRawMaterial> dispatchRawMaterials;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchProductionWorkOrder> dispatchProductionWorkOrders;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchEquipment> dispatchEquipments;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchMaintenanceWorkOrder> dispatchMaintenanceWorkOrders;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchSamplingLocation> dispatchSamplingLocations;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchInstrument> dispatchInstruments;
//
//    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<DispatchTestSubject> dispatchTestSubjects;
//
//}
