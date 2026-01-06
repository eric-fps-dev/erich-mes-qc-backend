//package com.fps.svmes.repositories.jpaRepo.user;
//
//import com.fps.svmes.models.sql.user.TeamForm;
//import com.fps.svmes.models.sql.user.TeamFormId;
//import com.fps.svmes.models.sql.user.TeamUserId;
//import jakarta.transaction.Transactional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface TeamFormRepository extends JpaRepository<TeamForm, TeamFormId> {
//    List<TeamForm> findByTeamId(Integer teamId);
//    boolean existsById(TeamFormId teamFormId);
//    void deleteByTeamId(Integer teamId);
//    void deleteById(TeamFormId teamFormId);
//
//    @Modifying
//    @Query("DELETE FROM TeamForm tf WHERE tf.id.formId IN :formIds")
//    void deleteAllByFormIds(@Param("formIds") List<String> formIds);
//
//    @Modifying
//    @Transactional
//    @Query("""
//       DELETE FROM TeamForm tf
//       WHERE tf.id.teamId = :teamId
//         AND tf.id.formId IN :formIds
//       """)
//    void deleteByTeamIdAndFormIdIn(Integer teamId, List<String> formIds);
//}
