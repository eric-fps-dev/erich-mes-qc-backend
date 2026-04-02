//package com.fps.svmes.dto.dtos.user;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.persistence.Column;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import java.time.OffsetTime;
//import java.util.List;
//
//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@EqualsAndHashCode(callSuper = true)
//public class TeamDTO extends CommonDTO {
//
//    @JsonProperty("id")
//    private Integer id;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("type")
//    private String type;
//
//    @JsonProperty("leader")
//    private UserDTO leader; // Add this for full user details.
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
//    private String description;
//
//    @JsonProperty("parent_id")
//    private Integer parentId;
//
//    @JsonProperty("children")
//    private List<TeamDTO> children;
//
//    private Integer level;
//
//    private Integer memberCount;
//
//    private Integer associatedFormCount;
//}
