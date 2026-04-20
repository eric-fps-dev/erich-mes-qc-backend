package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.shared.entity.primary.team.TeamQCForm;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface TeamFormRepository extends JpaRepository<TeamQCForm, TeamQCForm.TeamFormId> {

    List<TeamQCForm> findByTeamId(Integer teamId);

    boolean existsById(TeamQCForm.TeamFormId teamFormId);

    void deleteByTeamId(Integer teamId);

    void deleteById(TeamQCForm.TeamFormId teamFormId);

    @Modifying
    @Query("DELETE FROM TeamQCForm tf WHERE tf.id.formId IN :formIds")
    void deleteAllByFormIds(@Param("formIds") List<String> formIds);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM TeamQCForm tf
        WHERE tf.id.teamId = :teamId
          AND tf.id.formId IN :formIds
        """)
    void deleteByTeamIdAndFormIdIn(Integer teamId, List<String> formIds);

    @Query("SELECT tf.id.formId FROM TeamQCForm tf WHERE tf.id.teamId = :teamId")
    Set<String> findFormIdsByTeamId(Integer teamId);

    void deleteByIdTeamIdAndIdFormIdIn(Integer teamId, List<String> formIds);
}
