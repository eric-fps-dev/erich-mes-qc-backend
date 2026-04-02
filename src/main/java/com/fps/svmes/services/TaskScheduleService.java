//package com.fps.svmes.services;
//
//import com.fps.svmes.models.sql.taskSchedule.Dispatch;
//import com.fps.svmes.models.sql.taskSchedule.TaskType;
//
//import java.time.OffsetDateTime;
//import java.util.Map;
//
//public interface TaskScheduleService {
//
//    void scheduleDispatch(Dispatch dispatch, Runnable task);
//    void setupCronTask(Dispatch dispatch, Runnable task);
//    void setupCancelTask(Dispatch dispatch);
//    void scheduleFutureDispatch(Dispatch dispatch, Runnable task);
//    void scheduleCustomDispatch(Dispatch dispatch, Runnable task);
//    boolean isScheduled(Long dispatchId);
//    OffsetDateTime getNextExecutionTime(Long dispatchId, TaskType type);
//    Map<Long, Map<TaskType, OffsetDateTime>> getAllScheduledTasks();
//    boolean removeTask(long dispatchId, TaskType type);
//    boolean removeAllTasks(long dispatchId);
//}