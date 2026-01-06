//package com.fps.svmes.repositories.jpaRepo.dispatch;
//
//import com.fps.svmes.models.sql.taskSchedule.SamplingLocation;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface SamplingLocationRepository extends JpaRepository<SamplingLocation, Long> {
//    List<SamplingLocation> findByStatus(Integer status);
//    Optional<SamplingLocation> findByIdAndStatus(Long id, Integer status);
//}
