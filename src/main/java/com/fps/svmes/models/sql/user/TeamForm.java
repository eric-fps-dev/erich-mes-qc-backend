package com.fps.svmes.models.sql.user;

import com.fps.shared.entity.primary.team.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "team_form_ref", schema = "quality_management")
public class TeamForm {

    @EmbeddedId
    private TeamFormId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", referencedColumnName = "id")
    private Team team;
}
