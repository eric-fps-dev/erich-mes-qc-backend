package com.fps.svmes.models.sql.user;

import com.fps.shared.entity.primary.team.Team;
import com.fps.shared.entity.primary.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "team_user", schema = "quality_management")
public class TeamUser {

    @EmbeddedId
    private TeamUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    public TeamUser() {
    }

    public TeamUser(Integer userId, Integer teamId) {
        this.id = new TeamUserId(teamId, userId);
    }

    public TeamUser(TeamUserId teamUserId) {
        this.id = teamUserId;
    }
}
