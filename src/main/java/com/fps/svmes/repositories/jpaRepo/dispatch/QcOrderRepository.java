//package com.fps.svmes.repositories.jpaRepo.dispatch;
//
//import com.fps.svmes.models.sql.taskSchedule.Dispatch;
//import com.fps.svmes.models.sql.taskSchedule.QcOrder;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface QcOrderRepository extends JpaRepository<QcOrder, Long> {
//    Optional<QcOrder> findByIdAndStatus(Long id, Short status);
//
//}
