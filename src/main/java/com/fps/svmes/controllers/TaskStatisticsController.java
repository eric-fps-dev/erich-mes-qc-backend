//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.task.QuarterlyTaskStatisticsDTO;
//import com.fps.svmes.dto.dtos.task.TaskStateStatisticsDTO;
//import com.fps.svmes.dto.dtos.task.TaskStatisticsDTO;
//import com.fps.svmes.dto.dtos.task.TaskCountDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.DispatchedTaskService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/task-stats")
//@Slf4j
//@RequiredArgsConstructor
//@Tag(name = "Task Statistics API", description = "API for retrieving task statistics")
//public class TaskStatisticsController {
//
//    private final DispatchedTaskService dispatchedTaskService;
//
//    @GetMapping
//    @Operation(summary = "Get task statistics", description = "Fetch task statistics for a user")
//    public ResponseResult<TaskStatisticsDTO> getTaskStatistics(@RequestParam Long userId) {
//        try {
//            int todayTasks = dispatchedTaskService.getCurrentTasks(userId).size();
//            int futureTasks = dispatchedTaskService.getFutureTasks(userId).size();
//            int historicalTasks = dispatchedTaskService.getHistoricalTasks(userId).size();
//            int overdueTasks = dispatchedTaskService.getOverdueTasks(userId).size();
//
//            // TODO: Modify this Hardcoded percentage changes
//            TaskStatisticsDTO taskStatistics = new TaskStatisticsDTO(
//                    userId,
//                    new TaskCountDTO(todayTasks, 24),
//                    new TaskCountDTO(futureTasks, -12),
//                    new TaskCountDTO(historicalTasks, 16),
//                    new TaskCountDTO(overdueTasks, 16)
//            );
//
//            log.info("Task statistics retrieved for userId: {}", userId);
//            return ResponseResult.success(taskStatistics);
//        } catch (Exception e) {
//            log.error("Error retrieving task statistics for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve task statistics", e);
//        }
//    }
//
//    @GetMapping("/quarterly-stats")
//    @Operation(summary = "Get quarterly task statistics", description = "Fetch quarterly task statistics for a user")
//    public ResponseResult<QuarterlyTaskStatisticsDTO> getQuarterlyTaskStatistics(@RequestParam Long userId) {
//        try {
//            QuarterlyTaskStatisticsDTO quarterlyStatistics = dispatchedTaskService.getQuarterlyTaskStatistics(userId);
//            log.info("Quarterly task statistics retrieved for userId: {}", userId);
//            return ResponseResult.success(quarterlyStatistics);
//        } catch (Exception e) {
//            log.error("Error retrieving quarterly task statistics for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve quarterly task statistics", e);
//        }
//    }
//
//    @GetMapping("/state-stats")
//    @Operation(summary = "Get task state statistics", description = "Fetch task state statistics for a user")
//    public ResponseResult<TaskStateStatisticsDTO> getTaskStateStatistics(@RequestParam Long userId) {
//        try {
//            TaskStateStatisticsDTO statistics = dispatchedTaskService.getTaskStateStatistics(userId);
//            log.info("Task state statistics retrieved for userId: {}", userId);
//            return ResponseResult.success(statistics);
//        } catch (Exception e) {
//            log.error("Error retrieving task state statistics for userId: {}", userId, e);
//            return ResponseResult.fail("Failed to retrieve task state statistics", e);
//        }
//    }
//}
