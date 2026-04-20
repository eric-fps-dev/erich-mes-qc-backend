//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import com.fps.svmes.models.sql.user.User;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.OffsetDateTime;
//
///**
// * Represents a test dispatched to a specific personnel with updated fields.
// */
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "dispatched_task", schema = "quality_management")
//@Data
//public class DispatchedTask extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id")
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "dispatch_id")
//    private Dispatch dispatch; // Associated configuration
//
//    @ManyToOne
//    @JoinColumn(name = "user_id")
//    private User user;
//
//    @Column(name = "qc_form_tree_node_id")
//    private String qcFormTreeNodeId; // ID of the dispatched form
//
//    @Column(name = "dispatch_time", nullable = false, updatable = false)
//    @CreationTimestamp
//    private OffsetDateTime dispatchTime; // Time when the test was dispatched
//
//    @Column(name = "name", length = 255, nullable = true)
//    private String name; // Task name
//
//    @Column(name = "description", columnDefinition = "TEXT")
//    private String description; // Task description
//
//    @Column(name = "due_date", nullable = true)
//    private OffsetDateTime dueDate; // Due date of the task
//
//    @Column(name = "is_overdue", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
//    private Boolean isOverdue; // Indicates if the task is overdue
//
//    @Column(name = "dispatched_task_state_id", nullable = false)
//    private Short stateId; // State ID referencing dispatched_task_state
//
//    @Column(name = "finished_at")
//    private OffsetDateTime finishedAt; // Tracks when the task was finished
//
//    @Column(name = "notes")
//    private String notes; // Optional notes about edits or status changes
//
//}
