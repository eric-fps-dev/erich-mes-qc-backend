//package com.fps.svmes.dto.dtos.user;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//public class TeamForUserTableDTO {
//    @JsonProperty("id")
//    private Integer id;
//
//    @JsonProperty("team_name")
//    private String name;
//
//    @JsonProperty("leader_name")
//    private String leaderName; // Only store the leader's name for this
//
//    public TeamForUserTableDTO(Integer id, String name, String leaderName) {
//        this.id = id;
//        this.name = name;
//        this.leaderName = leaderName;
//    }
//}
