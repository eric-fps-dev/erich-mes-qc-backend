//package com.fps.svmes.models.sql.user;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Getter
//@Setter
//@EqualsAndHashCode(of = "id")
//@Entity
//@NoArgsConstructor
//@AllArgsConstructor
//@Table(name = "team_form", schema = "quality_management")
//public class TeamForm {
//    @EmbeddedId
//    TeamFormId id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @MapsId("teamId")
//    @JoinColumn(name = "team_id", referencedColumnName = "id")
//    private Team team;
//}