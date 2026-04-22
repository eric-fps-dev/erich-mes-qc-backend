//package com.fps.svmes.services;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
//import com.fps.svmes.dto.dtos.task.QuarterlyTaskStatisticsDTO;
//import com.fps.svmes.dto.dtos.task.TaskStateStatisticsDTO;
//import org.springframework.data.domain.Page;
//
//import java.util.List;
//
//public interface DispatchedTaskService {
//    List<DispatchedTaskDTO> getCurrentTasks(Long userId);
//    List<DispatchedTaskDTO> getFutureTasks(Long userId);
//    List<DispatchedTaskDTO> getHistoricalTasks(Long userId);
//    List<DispatchedTaskDTO> getOverdueTasks(Long userId);
//    void insertDispatchedTasks(DispatchedTaskDTO dispatchedTaskDTO, List<Integer> userIds);
//    void updateDispatchedTask(Long id, DispatchedTaskDTO dispatchedTaskDTO);
//    DispatchedTaskDTO getDispatchedTaskById(Long id);
//    void deleteDispatchedTask(Long id);
//    Page<DispatchedTaskDTO> getAllDispatchedTasks(int page, int size, String sort, String search);
//    Page<DispatchedTaskDTO> getDispatchedTasksByDispatchId(Long dispatchId, int page, int size, String sort, String search);
//    QuarterlyTaskStatisticsDTO getQuarterlyTaskStatistics(Long userId);
//    TaskStateStatisticsDTO getTaskStateStatistics(Long userId);
//}
