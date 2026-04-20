//package com.fps.svmes.models.sql.user;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//@Entity
//@Table(name = "user", schema = "quality_management")
//@Data
//@EqualsAndHashCode(callSuper = true)
//public class User extends Common {
//    @Id
//    @JsonProperty("id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Integer id;
//
//    @JsonProperty("name")
//    @Column(name = "name")
//    private String name;
//
//    @OneToOne
//    @JoinColumn(name = "role_id", referencedColumnName = "id")
//    @JsonProperty("role")
//    private Role role;
//
//    @JsonProperty("wecom_id")
//    @Column(name = "wecom_id")
//    private String wecomId;
//
//    @JsonProperty("username")
//    @Column(name = "username")
//    private String username;
//
//    @JsonProperty("password")
//    @Column(name = "password")
//    private String password;
//
//    @JsonProperty("email")
//    @Column(name = "email")
//    private String email;
//
//    @JsonProperty("phone_number")
//    @Column(name = "phone_number")
//    private String phoneNumber;
//
//    @JsonProperty("activation_status")
//    @Column(name = "activation_status")
//    private Short activationStatus;
//}
