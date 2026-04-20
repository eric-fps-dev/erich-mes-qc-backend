//package com.fps.svmes.models.sql.user;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import jakarta.persistence.*;
//import lombok.Data;
//
//@Entity
//@Table(name = "role", schema = "quality_management")
//@Data
//public class Role {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    @JsonProperty("id")
//    private Short id;
//
//    @Column(name = "name", nullable = false, unique = true)
//    @JsonProperty("name")
//    private String name;
//
//    @Column(name = "description")
//    @JsonProperty("description")
//    private String description;
//
//    @Column(name = "el_tag_type")
//    @JsonProperty("el_tag_type")
//    private String elTagType;
//}
//
