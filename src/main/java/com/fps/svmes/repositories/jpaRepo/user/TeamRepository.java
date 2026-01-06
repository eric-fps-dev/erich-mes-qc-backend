//package com.fps.svmes.repositories.jpaRepo.user;
//
//import com.fps.svmes.models.sql.user.Team;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface TeamRepository extends JpaRepository<Team, Integer> {
//    Optional<Team> findByLeaderId(Integer leaderId);
//    List<Team> findByParentIsNull();
//
//    @Query(value = """
//        WITH RECURSIVE t AS (
//             SELECT id
//               FROM quality_management.team
//              WHERE id = :root
//           UNION ALL
//             SELECT c.id
//               FROM quality_management.team c
//               JOIN t ON c.parent_id = t.id
//        )
//        SELECT id FROM t
//        """, nativeQuery = true)
//    List<Integer> findSelfAndDescendantIds(@Param("root") Integer rootId);
//
//    /**
//     * Return the id of team itself and every ancestor id (parent, grand-parent …) up
//     * to the root team.
//     */
//    @Query(value = """
//        WITH RECURSIVE ancestors AS (
//            -- start with the current (leaf) node
//            SELECT id, parent_id
//              FROM quality_management.team
//             WHERE id = :leafId
//
//            UNION ALL
//
//            -- walk upward: parent of previous row
//            SELECT t.id, t.parent_id
//              FROM quality_management.team t
//              JOIN ancestors a ON t.id = a.parent_id
//        )
//        SELECT id                  -- just the id column
//          FROM ancestors;
//        """,
//            nativeQuery = true)
//    List<Integer> findSelfAndAncestorIds(@Param("leafId") Integer leafId);
//}