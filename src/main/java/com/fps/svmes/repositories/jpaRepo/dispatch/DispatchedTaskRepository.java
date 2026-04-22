//package com.fps.svmes.repositories.jpaRepo.dispatch;
//
//import com.fps.svmes.models.sql.taskSchedule.DispatchedTask;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.data.jpa.repository.Query;
//
//import java.util.List;
//
//public interface DispatchedTaskRepository extends JpaRepository<DispatchedTask, Long>, JpaSpecificationExecutor<DispatchedTask> {
//
//    // Find tasks for the current day
//    List<DispatchedTask> findByUserIdAndDueDateBetweenAndStatus(
//            Long userId, java.time.OffsetDateTime startOfDay, java.time.OffsetDateTime endOfDay, Integer status);
//
//    // Find tasks with due dates after today (future tasks)
//    List<DispatchedTask> findByUserIdAndDueDateAfterAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);
//
//    // Find tasks with due dates before today (historical tasks)
//    List<DispatchedTask> findByUserIdAndDueDateBeforeAndStatus(Long userId, java.time.OffsetDateTime dueDate, Integer status);
//
//    // Find overdue tasks
//    List<DispatchedTask> findByUserIdAndIsOverdueAndStatus(Long userId, Boolean isOverdue, Integer status);
//
//    // Find tasks with dispatch id that are in pending
//    List<DispatchedTask> findByDispatchIdAndStateIdAndStatus(Long dispatchId, Integer stateId, Integer status);
//
//    Page<DispatchedTask> findAll(Pageable pageable);
//
//    @Query(value = """
//            SELECT COUNT(*) FROM quality_management.dispatched_task
//            WHERE user_id = :userId
//            AND EXTRACT(QUARTER FROM due_date) = :quarter
//            """, nativeQuery = true)
//    int countTasksByQuarter(Long userId, int quarter);
//
//    @Query(value = """
//                SELECT COUNT(*) FROM quality_management.dispatched_task
//                WHERE user_id = :userId AND dispatched_task_state_id = :stateId
//            """, nativeQuery = true)
//    int countTasksByState(Long userId, int stateId);
//
//
//
//}
