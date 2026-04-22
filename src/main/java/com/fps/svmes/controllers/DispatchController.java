//package com.fps.svmes.controllers;
//
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.models.sql.taskSchedule.Dispatch;
//import com.fps.svmes.models.sql.taskSchedule.TaskType;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
//import com.fps.svmes.services.DispatchService;
//import com.fps.svmes.services.TaskScheduleService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.validation.Valid;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.OffsetDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * REST Controller for managing dispatch configurations.
// */
//@RestController
//@RequestMapping("/dispatch")
//@Tag(name = "Dispatch API", description = "API for QC Dispatch Dispatch")
//public class DispatchController {
//
//    @Autowired
//    private DispatchService dispatchService;
//
//    @Autowired
//    private TaskScheduleService taskScheduleService;
//
//    @Autowired
//    private DispatchRepository dispatchRepository;
//
//    private static final Logger logger = LoggerFactory.getLogger(DispatchController.class);
//
//    @Operation(summary = "Create a new dispatch", description = "Creates a dispatch in the QC System")
//    @PostMapping
//    public ResponseResult<DispatchDTO> createDispatch(@RequestBody DispatchDTO request) {
//        try {
//            DispatchDTO dispatchDTO = dispatchService.convertToDispatchDTO(dispatchService.createDispatch(request));
//            return ResponseResult.success(dispatchDTO);
//        } catch (Exception e) {
//            logger.error("Error creating dispatch", e);
//            return ResponseResult.fail("Failed to create dispatch", e);
//        }
//    }
//
//    @Operation(summary = "Update an existing dispatch", description = "Updates a dispatch given an ID")
//    @PutMapping("/{id}")
//    public ResponseResult<DispatchDTO> updateDispatch(@PathVariable Long id, @RequestBody @Valid DispatchDTO request) {
//        try {
//            DispatchDTO dispatchDTO = dispatchService.convertToDispatchDTO(dispatchService.updateDispatch(id, request));
//            return ResponseResult.success(dispatchDTO);
//        } catch (Exception e) {
//            logger.error("Error updating dispatch with ID: {}",  id);
//            return ResponseResult.fail("Failed to update a dispatch", e);
//        }
//    }
//
//    @Operation(summary = "Delete a dispatch", description = "Deletes a dispatch given its ID")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteDispatch(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            dispatchService.deleteDispatch(id, userId);
//            return ResponseResult.success("Dispatch with ID " + id + " deleted successfully.");
//        } catch (Exception e) {
//            logger.error("Error deleting a dispatch with ID: {}", id );
//            return ResponseResult.fail("Failed to delete a dispatch", e);
//        }
//    }
//
//    @Operation(summary = "Get a single dispatch by ID", description = "Retrieves a dispatch by its ID")
//    @GetMapping("/{id}")
//    public ResponseResult<DispatchDTO> getDispatch(@PathVariable Long id) {
//        try {
//            DispatchDTO dispatchDTO = dispatchService.getDispatch(id);
//            return ResponseResult.success(dispatchDTO);
//        } catch (Exception e) {
//            logger.error("Error retrieving dispatch for ID: {}", id);
//            return ResponseResult.fail("Failed to retrieve dispatch", e);
//        }
//    }
//
//
//    @Operation(summary = "Get all dispatches", description = "Retrieves a list of all dispatches")
//    @GetMapping
//    public ResponseResult<List<DispatchDTO>> getAllDispatches() {
//        try {
//            List<DispatchDTO> dispatches = dispatchService.getAllDispatches();
//            return dispatches.isEmpty()
//                    ? ResponseResult.noContent(dispatches)
//                    : ResponseResult.success(dispatches);
//        } catch (Exception e) {
//            logger.error("Error retrieving all dispatches");
//            return ResponseResult.fail("Failed to retrieve all dispatches", e);
//        }
//    }
//
//    @Operation(summary = "Get dispatch by dispatched task ID", description = "Retrieves a dispatch using the dispatched task's ID")
//    @GetMapping("/dispatched-task/{dispatchedTaskId}")
//    public ResponseResult<DispatchDTO> getDispatchByDispatchedTaskId(@PathVariable Long dispatchedTaskId) {
//        try {
//            DispatchDTO dispatchDTO = dispatchService.getDispatchByDispatchedTaskId(dispatchedTaskId);
//            return ResponseResult.success(dispatchDTO);
//        } catch (Exception e) {
//            logger.error("Error retrieving dispatch for dispatched task ID: {}", dispatchedTaskId, e);
//            return ResponseResult.fail("Failed to retrieve dispatch for dispatched task ID: " + dispatchedTaskId, e);
//        }
//    }
//
//
//
//
//    /**
//     * Endpoint to schedule a dispatch task.
//     *
//     * @param id the ID of the dispatch to schedule.
//     * @return success or failure response.
//     */
//    @Operation(summary = "Schedule dispatch task for the specified dispatch", description = "Schedule dispatch task for the specified dispatch")
//    @PostMapping("/schedule/{id}")
//    public ResponseResult<String> scheduleTask(@PathVariable Long id) {
//        try {
//            Optional<Dispatch> dispatch = dispatchRepository.findByIdAndStatus(id, 1);
//            if (dispatch.isPresent()){
//                Long dispatch_id = dispatch.get().getId();
//                dispatchService.initializeDispatch(dispatch_id, () -> dispatchService.executeDispatch(dispatch_id));
//                return ResponseResult.success("Task scheduled successfully for Dispatch ID: " + id);
//            } else {
//                return ResponseResult.fail("Failed to schedule a dispatch's task, dispatch not found with ID: " + id);
//            }
//        } catch (Exception e) {
//            logger.error("Error scheduling a dispatch with ID: {}", id);
//            return ResponseResult.fail("Failed to schedule a dispatch", e);
//        }
//    }
//
//    @Operation(summary = "Pause a dispatch", description = "Pauses the specified dispatch and cancels its tasks.")
//    @PutMapping("/pause/{id}/{userId}")
//    public ResponseResult<String> pauseDispatch(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            dispatchService.pauseDispatch(id, userId);
//            return ResponseResult.success("Dispatch with ID " + id + " has been paused.");
//        } catch (EntityNotFoundException e) {
//            logger.error("Error pausing dispatch with ID: {}", id, e);
//            return ResponseResult.fail("Dispatch not found for ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error pausing dispatch with ID: {}", id, e);
//            return ResponseResult.fail("Failed to pause dispatch with ID: " + id, e);
//        }
//    }
//
//    @Operation(summary = "Resume a paused dispatch", description = "Resumes the specified dispatch and re-enables its tasks.")
//    @PutMapping("/resume/{id}/{userId}")
//    public ResponseResult<String> resumeDispatch(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            dispatchService.resumeDispatch(id, userId);
//            return ResponseResult.success("Dispatch with ID " + id + " has been resumed.");
//        } catch (EntityNotFoundException e) {
//            logger.error("Error resuming dispatch with ID: {}", id, e);
//            return ResponseResult.fail("Dispatch not found for ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error resuming dispatch with ID: {}", id, e);
//            return ResponseResult.fail("Failed to resume dispatch with ID: " + id, e);
//        }
//    }
//
//    /**
//     * Endpoint to cancel a scheduled task.
//     *
//     * @param id the ID of the dispatch to cancel.
//     * @return success or failure response.
//     */
//    @Operation(summary = "Cancel dispatch task for specified dispatch", description = "Schedule dispatch task for the specified dispatch")
//    @DeleteMapping("/cancel/{id}")
//    public ResponseResult<String> cancelTask(@PathVariable Long id) {
//
//        try {
//            dispatchService.cancelDispatchTask(id);
//            return ResponseResult.success("Task cancelled successfully for Dispatch ID: " + id);
//        }
//        catch (Exception e) {
//            logger.error("Error canceling a dispatch with ID: {}", id);
//            return ResponseResult.fail("Failed to cancel a dispatch", e);
//        }
//    }
//
//    /**
//     * Endpoint to check if a task is scheduled.
//     *
//     * @param id the ID of the dispatch to check.
//     * @return true if the task is scheduled, false otherwise.
//     */
//    @Operation(summary = "Check if a dispatch has any task scheduled", description = "Retrieves true or false on whether a dispatch has task scheduled")
//    @GetMapping("/is-scheduled/{id}")
//    public ResponseResult<Boolean> isTaskScheduled(@PathVariable Long id) {
//
//        try {
//            boolean isScheduled = taskScheduleService.isScheduled(id);
//            return ResponseResult.success(isScheduled);
//        } catch (Exception e) {
//            logger.error("Error checking is scheduled for a dispatch with ID: {}", id);
//            return ResponseResult.fail("Fail to check is scheduled for a dispatch with ID", e);
//        }
//    }
//
//
//    /**
//     * Endpoint to get the next execution time of a scheduled task.
//     *
//     * @param id the ID of the dispatch to check.
//     * @return the next execution time or null if not scheduled.
//     */
//    @Operation(summary = "Get next execution time on CRON task of a dispatch", description = "Get next execution time on CRON task of a dispatch, null if no cron task is scheduled")
//    @GetMapping("/next-execution-time/{id}")
//    public ResponseResult<OffsetDateTime> getNextExecutionTime(@PathVariable Long id) {
//        try {
//            OffsetDateTime nextExecutionTime = taskScheduleService.getNextExecutionTime(id, TaskType.CRON);
//            return ResponseResult.success(nextExecutionTime);
//        } catch (Exception e) {
//            logger.error("Error getting next execution time for dispatch ID: {}", id);
//            return ResponseResult.fail("Fail to get next execution time for dispatch with ID", e);
//        }
//    }
//
//    @Operation(summary = "Get all scheduled tasks", description = "Retrieves all scheduled tasks and their next execution times.")
//    @GetMapping("/scheduled-tasks")
//    public ResponseResult<List<Map<String, Object>>> getAllScheduledTasks() {
//        try {
//            Map<Long, Map<TaskType, OffsetDateTime>> scheduledTasks = taskScheduleService.getAllScheduledTasks();
//
//            // Convert Map to List of Maps for JSON-friendly representation
//            List<Map<String, Object>> response = scheduledTasks.entrySet().stream()
//                    .map(dispatchEntry -> {
//                        Map<String, Object> dispatchInfo = new HashMap<>();
//
//                        // Convert the inner map of TaskType -> OffsetDateTime into a list for easier representation
//                        List<Map<String, Object>> taskDetails = dispatchEntry.getValue().entrySet().stream()
//                                .map(taskEntry -> {
//                                    Map<String, Object> taskInfo = new HashMap<>();
//                                    taskInfo.put("taskType", taskEntry.getKey().name()); // Use name() for enum
//                                    taskInfo.put("nextExecutionTime", taskEntry.getValue());
//                                    return taskInfo;
//                                })
//                                .toList();
//
//                        dispatchInfo.put("tasks", taskDetails); // Add task details as a list
//                        dispatchInfo.put("dispatchId", dispatchEntry.getKey());
//
//                        return dispatchInfo;
//                    })
//                    .toList();
//
//            return response.isEmpty()
//                    ? ResponseResult.noContent(response)
//                    : ResponseResult.success(response);
//        } catch (Exception e) {
//            logger.error("Error retrieving all scheduled tasks details", e);
//            return ResponseResult.fail("Failed to retrieve all scheduled tasks", e);
//        }
//    }
//
//    @Operation(summary = "Convert spring CRON expression to Chinese", description = "Parses a Spring CRON expression and returns a human-readable description in Chinese")
//    @GetMapping("/parse-cron")
//    public ResponseResult<String> parseCronToChinese(@RequestParam String cronExpression) {
//        try {
//            String result = dispatchService.parseSpringCronToChinese(cronExpression);
//            return ResponseResult.success(result);
//        } catch (Exception e) {
//            logger.error("Error parsing cron expression: {}", cronExpression, e);
//            return ResponseResult.fail("Failed to parse cron expression", e);
//        }
//    }
//}
//
//
//
