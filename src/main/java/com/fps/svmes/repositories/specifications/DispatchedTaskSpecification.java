//package com.fps.svmes.repositories.specifications;
//
//import com.fps.svmes.models.sql.taskSchedule.DispatchedTask;
//import com.fps.svmes.models.sql.user.User;
//import com.fps.svmes.utils.DispatchedTaskStateMapper;
//import jakarta.persistence.criteria.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//
//public class DispatchedTaskSpecification {
//
//
//    public static Specification<DispatchedTask> byDispatchId(Long dispatchId) {
//        return (root, query, cb) -> cb.equal(root.get("dispatch").get("id"), dispatchId);
//    }
//
//    private static Predicate stateNamePredicate(Root<DispatchedTask> root, CriteriaBuilder cb, String keyword) {
//        List<Predicate> predicates = new ArrayList<>();
//
//        // Iterate over all possible state mappings and check if the keyword is contained
//        for (Map.Entry<Short, String> entry : DispatchedTaskStateMapper.getAllStateMappings().entrySet()) {
//            String stateName = entry.getValue().toLowerCase();
//            Short stateId = entry.getKey();
//
//            if (stateName.contains(keyword.toLowerCase())) {
//                predicates.add(cb.equal(root.get("stateId"), stateId));
//            }
//        }
//
//        // Return OR condition if matches exist, otherwise return an empty predicate
//        return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(new Predicate[0]));
//    }
//
//
//    // specification is an interface that used to created dynamic queries using Criteria API
//    // root(table source), query, cb(criteria builder), used to construct WHERE conditions dynamically
//    public static Specification<DispatchedTask> searchByKeyword(String keyword, List<String> matchingFormIds) {
//        return (root, query, cb) -> {
//            if (keyword == null || keyword.isEmpty()) {
//                return cb.conjunction();
//            }
//
//            List<Predicate> predicates = new ArrayList<>();
//
//            // Search by Task Fields
//            predicates.add(cb.like(cb.lower(root.get("id").as(String.class)), "%" + keyword.toLowerCase() + "%"));
//            predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
//            predicates.add(cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%"));
//            predicates.add(cb.like(cb.lower(root.get("notes")), "%" + keyword.toLowerCase() + "%"));
//            predicates.add(cb.like(cb.lower(root.get("dispatch").get("id").as(String.class)), "%" + keyword.toLowerCase() + "%"));
//
//            // Search by Date Fields (Convert to String)
//            predicates.add(cb.like(cb.lower(root.get("dispatchTime").as(String.class)), "%" + keyword.toLowerCase() + "%"));
//            predicates.add(cb.like(cb.lower(root.get("dueDate").as(String.class)), "%" + keyword.toLowerCase() + "%"));
//
//            // Search by User Name (Join User Table)
//            Join<DispatchedTask, User> userJoin = root.join("user", JoinType.LEFT);
//            predicates.add(cb.like(cb.lower(userJoin.get("name")), "%" + keyword.toLowerCase() + "%"));
//
//            // Search by QC Form Tree Node (ID Matching from MongoDB)
//            if (!matchingFormIds.isEmpty()) {
//                predicates.add(root.get("qcFormTreeNodeId").in(matchingFormIds));
//            }
//
//            // Search by State Name (Partial Match)
//            predicates.add(stateNamePredicate(root, cb, keyword));
//
//            return cb.or(predicates.toArray(new Predicate[0]));
//        };
//    }
//}
//
