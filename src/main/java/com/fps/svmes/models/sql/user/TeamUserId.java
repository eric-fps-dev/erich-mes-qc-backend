package com.fps.svmes.models.sql.user;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
@Embeddable
public class TeamUserId implements Serializable {

    private Integer teamId;
    private Integer userId;

    public TeamUserId() {
    }

    public TeamUserId(Integer teamId, Integer userId) {
        this.teamId = teamId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamUserId that = (TeamUserId) o;
        return Objects.equals(teamId, that.teamId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, userId);
    }
}
