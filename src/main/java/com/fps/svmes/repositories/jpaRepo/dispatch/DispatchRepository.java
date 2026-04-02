//package com.fps.svmes.repositories.jpaRepo.dispatch;
//import java.util.List;
//import com.fps.svmes.models.sql.taskSchedule.Dispatch;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
///**
// * Repository for managing DispatchConfiguration entities.
// */
//public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
//    List<Dispatch> findByStatus(int i);
//
//    Optional<Dispatch> findByIdAndStatus(Long id, int status);
//
//    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.dispatchForms WHERE d.id = :id")
//    Optional<Dispatch> findWithFormsById(@Param("id") Long id);
//
//    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.dispatchUsers WHERE d.id = :id")
//    Optional<Dispatch> findWithUsersById(@Param("id") Long id);
//}
