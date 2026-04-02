//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "dispatch_test_subject", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchTestSubject extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "dispatch_id")
//    private Dispatch dispatch;
//
//    @ManyToOne
//    @JoinColumn(name = "test_subject_id")
//    private TestSubject testSubject;
//}
