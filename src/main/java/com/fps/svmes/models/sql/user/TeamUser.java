//package com.fps.svmes.models.sql.user;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//@Setter
//@Getter
//@Entity
//@Table(name = "team_user", schema = "quality_management")
//public class TeamUser {
//    @EmbeddedId
//    private TeamUserId id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @MapsId("teamId") // This tells Hibernate to use the teamId from the composite key
//    @JoinColumn(name = "team_id", referencedColumnName = "id")
//    private Team team;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @MapsId("userId") // This tells Hibernate to use the userId from the composite key
//    @JoinColumn(name = "user_id", referencedColumnName = "id")
//    private User user;
//
//    public TeamUser() {}
//
//    public TeamUser(Integer userId, Integer teamId) {
//        this.id = new TeamUserId(teamId, userId);
//    }
//
//    public TeamUser(TeamUserId teamUserId) {
//        this.id = teamUserId;
//    }
//}
//
//
