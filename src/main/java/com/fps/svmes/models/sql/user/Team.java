//package com.fps.svmes.models.sql.user;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fasterxml.jackson.annotation.JsonManagedReference;
//import io.swagger.v3.oas.annotations.media.Schema;
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import java.time.OffsetTime;
//import java.util.List;
//
//@Entity
//@Table(name = "team", schema = "quality_management")
//@Data
//@EqualsAndHashCode(callSuper = true)
//public class Team extends Common {
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
//    @JsonProperty("type")
//    @Column(name = "type")
//    private String type;
//
//    @OneToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "leader_id", referencedColumnName = "id")
//    private User leader;
//
//    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "14:30:00+02:00")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
//    @JsonProperty("start_time")
//    @Column(name = "start_time")
//    private OffsetTime startTime;
//
//    @Schema(type = "string", pattern = "HH:mm:ssXXX", example = "14:30:00+02:00")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
//    @JsonProperty("end_time")
//    @Column(name = "end_time")
//    private OffsetTime endTime;
//
//    @JsonProperty("description")
//    @Column(name = "description")
//    private String description;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "parent_id")
//    @JsonBackReference
//    private Team parent;
//
//    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference
//    private List<Team> children;
//}
