package com.fps.svmes.repositories.jpaRepo.user;

import com.fps.shared.entity.primary.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u.fullName FROM User u WHERE u.id = :id")
    String findNameById(@Param("id") Integer id);

    List<User> findAllByIdIn(Collection<Integer> ids);
}
