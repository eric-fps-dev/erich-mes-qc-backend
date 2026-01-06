//package com.fps.svmes.repositories.jpaRepo.dispatch;
//
//import com.fps.svmes.models.sql.taskSchedule.TestSubject;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface TestSubjectRepository extends JpaRepository<TestSubject, Long> {
//    List<TestSubject> findByStatus(Integer status);
//    Optional<TestSubject> findByIdAndStatus(Long id, Integer status);
//}
