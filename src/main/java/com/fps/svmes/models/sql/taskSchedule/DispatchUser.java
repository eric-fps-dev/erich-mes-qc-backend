//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fps.svmes.models.sql.Common;
//import com.fps.svmes.models.sql.user.User;
//import jakarta.persistence.*;
//import lombok.Data;
//
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "dispatch_user", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchUser extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "dispatch_id", nullable = false)
//    @JsonBackReference
//    private Dispatch dispatch;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//}
//
