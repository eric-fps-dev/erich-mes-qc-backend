//package com.fps.svmes.dto.dtos.dispatch;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import java.time.OffsetDateTime;
//
//@Data
//@EqualsAndHashCode(callSuper = true)
//public class DispatchedTaskDTO extends CommonDTO {
//
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("dispatch_id") // make it nullable
//    private Long dispatchId; // Associated configuration ID
//
//    @JsonProperty("user_id")
//    private Long userId; // User ID associated with the task
//
//    @JsonProperty("qc_form_tree_node_id")
//    private String qcFormTreeNodeId; // ID of the dispatched form
//
//    @JsonProperty("dispatch_time")
//    private OffsetDateTime dispatchTime; // Time when the test was dispatched
//
//    @JsonProperty("name")
//    private String name; // Task name
//
//    @JsonProperty("description")
//    private String description; // Task description
//
//    @JsonProperty("due_date")
//    private OffsetDateTime dueDate; // Due date of the task
//
//    @JsonProperty("is_overdue")
//    private Boolean isOverdue; // Indicates if the task is overdue
//
//    @JsonProperty("dispatched_task_state_id")
//    private Short stateId; // State ID referencing dispatched_task_state
//
//    @JsonProperty("finished_at")
//    private OffsetDateTime finishedAt; // Tracks when the task was finished
//
//    @JsonProperty("notes")
//    private String notes; // Optional notes about edits or status changes
//}
