//package com.fps.svmes.dto.dtos.dispatch;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import java.util.List;
//
//@EqualsAndHashCode(callSuper = true)
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@Data
//public class QcOrderDTO extends CommonDTO {
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("state")
//    private Short state;
//
//    @JsonProperty("description")
//    private String description;
//
//    @JsonProperty("dispatches")
//    private List<DispatchDTO> dispatches;
//}
