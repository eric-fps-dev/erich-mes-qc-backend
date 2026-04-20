//package com.fps.svmes.models.sql.user;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.models.sql.Common;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.OffsetTime;
//
//@Entity
//@Getter
//@Setter
//@Table(name = "Shift", schema = "quality_management")
//@EqualsAndHashCode(callSuper = true)
//@NoArgsConstructor
//@AllArgsConstructor
//public class Shift extends Common {
//    @Id
//    @JsonProperty("id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @JsonProperty("name")
//    @Column(name = "name")
//    private String name;
//
//    @JsonProperty("description")
//    @Column(name = "description")
//    private String description;
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
//    @JsonProperty("grace_minute")
//    @Column(name = "grace_minute")
//    private Integer graceMinute;
//}
