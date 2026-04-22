//package com.fps.svmes.dto.dtos.user;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.persistence.Column;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import java.time.OffsetTime;
//
//@EqualsAndHashCode(callSuper = true)
//@Data
//public class ShiftDTO extends CommonDTO {
//
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("description")
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
//    private Integer graceMinute;
//}
//
