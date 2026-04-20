/*package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.svmes.dto.dtos.user.TeamForUserTableDTO;
import com.fps.svmes.models.sql.user.TeamUser;
import com.fps.svmes.models.sql.user.TeamUserId;
import feign.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamUserRepository extends JpaRepository<TeamUser, Integer> {

    List<TeamUser> findByIdTeamId(Integer teamId);

    List<TeamUser> findByIdUserId(Integer userId);

    void deleteByIdUserId(Integer userId);

    void deleteByIdTeamId(Integer teamId);

    void deleteById(TeamUserId teamUserId);

    @Query("""
        SELECT new com.fps.svmes.dto.dtos.user.TeamForUserTableDTO(s.id, s.name, l.name)
        FROM TeamUser su
        JOIN su.team s
        LEFT JOIN s.leader l
        WHERE su.id.userId = :userId
        """)
    List<TeamForUserTableDTO> findTeamsByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM TeamUser tu
        WHERE tu.id.teamId = :teamId
          AND tu.id.userId IN :userIds
        """)
    void deleteByTeamIdAndUserIdIn(Integer teamId, List<Integer> userIds);
}*/
