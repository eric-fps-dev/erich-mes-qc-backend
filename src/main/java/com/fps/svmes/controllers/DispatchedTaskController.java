//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.DispatchedTaskService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.Page;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/dispatched-tasks")
//@Slf4j
//@RequiredArgsConstructor
//@Tag(name = "Dispatched Task API", description = "API for managing dispatched tasks")
//public class DispatchedTaskController {
//
//    private final DispatchedTaskService dispatchedTaskService;
//    private static final Logger logger = LoggerFactory.getLogger(DispatchedTaskController.class);
//
//    @GetMapping("/today")
//    @Operation(summary = "Get current tasks", description = "Fetch tasks assigned to a user for today.")
//    public ResponseResult<List<DispatchedTaskDTO>> getCurrentTasks(@RequestParam Long userId) {
//        try {
//            List<DispatchedTaskDTO> tasks = dispatchedTaskService.getCurrentTasks(userId);
//            logger.info("Current tasks retrieved for userId: {}, count: {}", userId, tasks.size());
//            return ResponseResult.success(tasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving current tasks for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve current tasks", e);
//        }
//    }
//
//    @GetMapping("/future")
//    @Operation(summary = "Get future tasks", description = "Fetch tasks assigned to a user with due dates after today.")
//    public ResponseResult<List<DispatchedTaskDTO>> getFutureTasks(@RequestParam Long userId) {
//        try {
//            List<DispatchedTaskDTO> tasks = dispatchedTaskService.getFutureTasks(userId);
//            logger.info("Future tasks retrieved for userId: {}, count: {}", userId, tasks.size());
//            return ResponseResult.success(tasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving future tasks for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve future tasks", e);
//        }
//    }
//
//    @GetMapping("/history")
//    @Operation(summary = "Get historical tasks", description = "Fetch tasks assigned to a user with due dates before today.")
//    public ResponseResult<List<DispatchedTaskDTO>> getHistoricalTasks(@RequestParam Long userId) {
//        try {
//            List<DispatchedTaskDTO> tasks = dispatchedTaskService.getHistoricalTasks(userId);
//            logger.info("Historical tasks retrieved for userId: {}, count: {}", userId, tasks.size());
//            return ResponseResult.success(tasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving historical tasks for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve historical tasks", e);
//        }
//    }
//
//    @GetMapping("/overdue")
//    @Operation(summary = "Get overdue tasks", description = "Fetch tasks assigned to a user that are overdue.")
//    public ResponseResult<List<DispatchedTaskDTO>> getOverdueTasks(@RequestParam Long userId) {
//        try {
//            List<DispatchedTaskDTO> tasks = dispatchedTaskService.getOverdueTasks(userId);
//            logger.info("Overdue tasks retrieved for userId: {}, count: {}", userId, tasks.size());
//            return ResponseResult.success(tasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving overdue tasks for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve overdue tasks", e);
//        }
//    }
//
//    @PostMapping("/insert")
//    @Operation(summary = "Insert dispatched tasks", description = "Insert multiple dispatched tasks based on user IDs")
//    public ResponseResult<Void> insertDispatchedTasks(@RequestBody DispatchedTaskDTO dispatchedTaskDTO,
//                                                      @RequestParam List<Integer> userIds) {
//        try {
//            dispatchedTaskService.insertDispatchedTasks(dispatchedTaskDTO, userIds);
//            logger.info("Inserted tasks for userIds: {}", userIds);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error inserting dispatched tasks", e);
//            return ResponseResult.fail("Failed to insert dispatched tasks", e);
//        }
//    }
//
//    @PutMapping("/update/{id}")
//    @Operation(summary = "Update a dispatched task", description = "Update a dispatched task by its ID (partial updates allowed)")
//    public ResponseResult<Void> updateDispatchedTask(@PathVariable Long id, @RequestBody DispatchedTaskDTO dispatchedTaskDTO) {
//        try {
//            dispatchedTaskService.updateDispatchedTask(id, dispatchedTaskDTO);
//            logger.info("Updated task with ID: {}", id);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error updating dispatched task with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update dispatched task", e);
//        }
//    }
//
//    @GetMapping("/get/{id}")
//    @Operation(summary = "Get a dispatched task", description = "Get a dispatched task by its ID")
//    public ResponseResult<DispatchedTaskDTO> getDispatchedTaskById(@PathVariable Long id) {
//        try {
//            DispatchedTaskDTO task = dispatchedTaskService.getDispatchedTaskById(id);
//            logger.info("Retrieved task with ID: {}", id);
//            return ResponseResult.success(task);
//        } catch (Exception e) {
//            logger.error("Error retrieving dispatched task with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve dispatched task", e);
//        }
//    }
//
//    @DeleteMapping("/delete/{id}")
//    @Operation(summary = "Delete a dispatched task", description = "Soft delete a dispatched task by its ID")
//    public ResponseResult<Void> deleteDispatchedTask(@PathVariable Long id) {
//        try {
//            dispatchedTaskService.deleteDispatchedTask(id);
//            logger.info("Deleted (soft) task with ID: {}", id);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error deleting dispatched task with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete dispatched task", e);
//        }
//    }
//
//    @GetMapping
//    @Operation(summary = "Get all dispatched tasks", description = "Retrieves a list of all dispatched tests")
//    public ResponseResult<Page<DispatchedTaskDTO>> getAllDispatchedTask(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "50") int size,
//            @RequestParam(defaultValue = "dispatch_time,desc") String sort,
//            @RequestParam(required = false) String search) {
//        try {
//            Page<DispatchedTaskDTO> tasks = dispatchedTaskService.getAllDispatchedTasks(page, size, sort, search);
//            return tasks.isEmpty()
//                    ? ResponseResult.noContent(tasks)
//                    : ResponseResult.success(tasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving all dispatched tasks");
//            return ResponseResult.fail("Failed to retrieve all dispatched tasks", e);
//        }
//    }
//
//    @GetMapping("/by-dispatch")
//    public ResponseResult<Page<DispatchedTaskDTO>> getDispatchedTasksByDispatchId(
//            @RequestParam Long dispatchId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "50") int size,
//            @RequestParam(defaultValue = "dispatch_time,desc") String sort,
//            @RequestParam(required = false) String search) {
//
//        try {
//            Page<DispatchedTaskDTO> dispatchedTasks = dispatchedTaskService.getDispatchedTasksByDispatchId(dispatchId, page, size, sort, search);
//            return ResponseResult.success(dispatchedTasks);
//        } catch (Exception e) {
//            logger.error("Error retrieving dispatched tasks by dispatch ID", e);
//            return ResponseResult.fail("Failed to retrieve dispatched tasks by dispatch ID", e);
//        }
//    }
//}
