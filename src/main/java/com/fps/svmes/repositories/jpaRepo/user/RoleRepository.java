package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.shared.entity.primary.rbac.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    @EntityGraph(attributePaths = {"children"})
    Optional<Role> findWithChildrenById(Integer id);
}
