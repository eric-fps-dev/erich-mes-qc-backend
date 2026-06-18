package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.shared.entity.primary.team.TeamFormAccessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamFormAccessConfigRepository extends JpaRepository<TeamFormAccessConfig, Integer> {
}
