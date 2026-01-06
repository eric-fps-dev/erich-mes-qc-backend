//package com.fps.svmes.services;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
//import com.fps.svmes.models.sql.taskSchedule.Dispatch;
//import jakarta.validation.Valid;
//
//import java.util.List;
//
///**
// * Service interface for handling dispatch logic.
// */
//public interface DispatchService {
//    void executeDispatch(Long dispatchId);
//    void scheduleDispatches();
//    Dispatch createDispatch(@Valid DispatchDTO request);
//    Dispatch updateDispatch(Long id, @Valid DispatchDTO request);
//    DispatchDTO getDispatch(Long id);
//    DispatchDTO getDispatchByDispatchedTaskId(Long dispatchedTaskId);
//    List<DispatchDTO> getAllDispatches();
//    void pauseDispatch(Long dispatchId, Integer userId);
//    void resumeDispatch(Long dispatchId, Integer userId);
//    void deleteDispatch(Long id, Integer userId);
//    void initializeDispatch(Long dispatchId, Runnable task);
//    void cancelDispatchTask(Long dispatchId);
//    DispatchDTO convertToDispatchDTO(Dispatch dispatch);
//    String parseSpringCronToChinese(String cronExpression);
//}
