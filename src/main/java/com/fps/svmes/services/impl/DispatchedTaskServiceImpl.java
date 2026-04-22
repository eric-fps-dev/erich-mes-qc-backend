//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchedTaskDTO;
//import com.fps.svmes.dto.dtos.task.QuarterlyTaskStatisticsDTO;
//import com.fps.svmes.dto.dtos.task.TaskStateStatisticsDTO;
//import com.fps.svmes.models.sql.taskSchedule.DispatchedTask;
//import com.fps.svmes.models.sql.user.User;
//import com.fps.svmes.models.nosql.FormNode;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
//import com.fps.svmes.services.DispatchedTaskService;
//import com.fps.svmes.services.FormNodeService;
//import com.fps.svmes.repositories.specifications.DispatchedTaskSpecification;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.time.OffsetDateTime;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//public class DispatchedTaskServiceImpl implements DispatchedTaskService {
//
//    @Autowired
//    private DispatchedTaskRepository dispatchedTaskRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private FormNodeService formNodeService;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public List<DispatchedTaskDTO> getCurrentTasks(Long userId) {
//        ZoneId chinaZone = ZoneId.of("Asia/Shanghai"); // Set timezone to China (UTC+8)
//        OffsetDateTime startOfDay = LocalDateTime.now(chinaZone).toLocalDate().atStartOfDay(chinaZone).toOffsetDateTime();
//        OffsetDateTime endOfDay = startOfDay.plusDays(1);
//
//        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateBetweenAndStatus(userId, startOfDay, endOfDay, 1);
//        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<DispatchedTaskDTO> getFutureTasks(Long userId) {
//        OffsetDateTime now = OffsetDateTime.now();
//        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateAfterAndStatus(userId, now, 1);
//        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<DispatchedTaskDTO> getHistoricalTasks(Long userId) {
//        OffsetDateTime now = OffsetDateTime.now();
//        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndDueDateBeforeAndStatus(userId, now, 1);
//        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<DispatchedTaskDTO> getOverdueTasks(Long userId) {
//        List<DispatchedTask> tasks = dispatchedTaskRepository.findByUserIdAndIsOverdueAndStatus(userId, true, 1);
//        return tasks.stream().map(task -> modelMapper.map(task, DispatchedTaskDTO.class)).collect(Collectors.toList());
//    }
//
//    @Override
//    public void insertDispatchedTasks(DispatchedTaskDTO dispatchedTaskDTO, List<Integer> userIds) {
//        for (Integer userId : userIds) {
//            DispatchedTask task = modelMapper.map(dispatchedTaskDTO, DispatchedTask.class);
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
//            task.setUser(user);
//            dispatchedTaskRepository.save(task);
//        }
//        sendWeComNotification(dispatchedTaskDTO);
//    }
//
//    private void sendWeComNotification(DispatchedTaskDTO task) { // only test for now
//        String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=npSmdfRWezU7zMHXBXNEG0M-9p1BVp_Y_qPGuoRxY54NIsYJZKcNhwSsYkQgLagOTxHMPQA-JlHljpvUj2lM7OmOs-8gx1NSG3beRrK4LVfoR098FmHOx7A0dw-oV6hGcD9bSEjhMLiUzJ0IA2KlkP9oR1Mp2srRv4YwW-X5UQBj5WCmmkaG_2P8E7EWo7B2_VnpXXO_VbfL8ZPXeKxSkg";
//
//        String payload = "{\n" +
//                "  \"touser\" : \"erik\",\n" +
//                "  \"msgtype\": \"markdown\",\n" +
//                "  \"agentid\" : 1000002,\n" +
//                "  \"markdown\": {\n" +
//                "       \"content\": \"您有一个新的QC派遣单：\\n>\\n>**任务详情**  \\n>\\n>任务单号：<font color=\\\"info\\\">" + task.getId() + "</font>  \\n>任务名称：<font color=\\\"info\\\">" + task.getName() + "</font>  \\n>负责人：<font color=\\\"info\\\">" + "Erik Yu" + "</font>  \\n>开始日期：<font color=\\\"info\\\">" + task.getDispatchTime() + "</font>  \\n>截止日期：<font color=\\\"warning\\\">" + task.getDueDate() + "</font>  \\n>  \\n> \\n>请点击：[QC质检任务](http://10.10.12.68:3000/form-display/29?usable=true&switchDisplayed=false&dispatchedTaskId=7208) 开始做任务：\"\n" +
//                "  },\n" +
//                "  \"enable_duplicate_check\": 0,\n" +
//                "  \"duplicate_check_interval\": 1800\n" +
//                "}";
//
//        try {
//            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setDoOutput(true);
//
//            try (OutputStream os = connection.getOutputStream()) {
//                os.write(payload.getBytes(StandardCharsets.UTF_8));
//                os.flush();
//            }
//
//            int responseCode = connection.getResponseCode();
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                // System.out.println("Notification sent successfully.");
//            } else {
//                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
//                    StringBuilder response = new StringBuilder();
//                    String line;
//                    while ((line = br.readLine()) != null) {
//                        response.append(line);
//                    }
//                    throw new RuntimeException("Failed to send notification. Response: " + response.toString());
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Error while sending WeCom notification: " + e.getMessage(), e);
//        }
//    }
//
//    @Override
//    public void updateDispatchedTask(Long id, DispatchedTaskDTO dispatchedTaskDTO) {
//        // Fetch the existing task or throw an exception if not found
//        DispatchedTask existingTask = dispatchedTaskRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
//
//        if (dispatchedTaskDTO.getName() != null) {
//            existingTask.setName(dispatchedTaskDTO.getName());
//        }
//        if (dispatchedTaskDTO.getDescription() != null) {
//            existingTask.setDescription(dispatchedTaskDTO.getDescription());
//        }
//        if (dispatchedTaskDTO.getDueDate() != null) {
//            existingTask.setDueDate(dispatchedTaskDTO.getDueDate());
//        }
//        if (dispatchedTaskDTO.getStateId() != null) {
//            existingTask.setStateId(dispatchedTaskDTO.getStateId());
//        }
//        if (dispatchedTaskDTO.getFinishedAt() != null) {
//            existingTask.setFinishedAt(dispatchedTaskDTO.getFinishedAt());
//        }
//        if (dispatchedTaskDTO.getNotes() != null) {
//            existingTask.setNotes(dispatchedTaskDTO.getNotes());
//        }
//        if (dispatchedTaskDTO.getIsOverdue() != null) {
//            existingTask.setIsOverdue(dispatchedTaskDTO.getIsOverdue());
//        }
//
//        // Save the updated task back to the database
//        dispatchedTaskRepository.save(existingTask);
//    }
//
//    @Override
//    public DispatchedTaskDTO getDispatchedTaskById(Long id) {
//        DispatchedTask task = dispatchedTaskRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
//        return modelMapper.map(task, DispatchedTaskDTO.class);
//    }
//
//    @Override
//    public void deleteDispatchedTask(Long id) {
//        DispatchedTask task = dispatchedTaskRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + id));
//        task.setStatus(0); // Set status to 0 for soft delete
//        dispatchedTaskRepository.save(task);
//    }
//
//    @Override
//    public Page<DispatchedTaskDTO> getAllDispatchedTasks(int page, int size, String sort, String search) {
//        String mappedSortField = mapDtoToEntityField(sort.split(",")[0]);
//        String sortDirection = sort.split(",")[1];
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), mappedSortField));
//
//        Specification<DispatchedTask> spec = Specification.where(null);
//
//        if (search != null && !search.trim().isEmpty()) {
//            // Find matching QC Form Nodes in MongoDB
//            List<String> matchingFormIds;
//
//            matchingFormIds = formNodeService.getNodesWithLabelContaining(search).stream().map(FormNode::getId).toList();
//            spec = spec.and(DispatchedTaskSpecification.searchByKeyword(search, matchingFormIds));
//        }
//
//        Page<DispatchedTask> dispatchedTaskPage = dispatchedTaskRepository.findAll(spec, pageable);
//
//        return dispatchedTaskPage.map(task -> modelMapper.map(task, DispatchedTaskDTO.class));
//    }
//
//    @Override
//    public Page<DispatchedTaskDTO> getDispatchedTasksByDispatchId(Long dispatchId, int page, int size, String sort, String search) {
//        String mappedSortField = mapDtoToEntityField(sort.split(",")[0]);
//        String sortDirection = sort.split(",")[1];
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), mappedSortField));
//
//        // Build Specification for SQL query
//        Specification<DispatchedTask> spec = Specification.where(DispatchedTaskSpecification.byDispatchId(dispatchId));
//
//        if (search != null && !search.trim().isEmpty()) {
//            // Find matching QC Form Nodes in MongoDB
//            List<String> matchingFormIds;
//
//            matchingFormIds = formNodeService.getNodesWithLabelContaining(search).stream().map(FormNode::getId).toList();
//            spec = spec.and(DispatchedTaskSpecification.searchByKeyword(search, matchingFormIds));
//        }
//
//        Page<DispatchedTask> dispatchedTaskPage = dispatchedTaskRepository.findAll(spec, pageable);
//
//        return dispatchedTaskPage.map(task -> modelMapper.map(task, DispatchedTaskDTO.class));
//    }
//
//    @Override
//    public QuarterlyTaskStatisticsDTO getQuarterlyTaskStatistics(Long userId) {
//        Map<String, Integer> quarterlyTasks = new HashMap<>();
//        quarterlyTasks.put("Q1", dispatchedTaskRepository.countTasksByQuarter(userId, 1));
//        quarterlyTasks.put("Q2", dispatchedTaskRepository.countTasksByQuarter(userId, 2));
//        quarterlyTasks.put("Q3", dispatchedTaskRepository.countTasksByQuarter(userId, 3));
//        quarterlyTasks.put("Q4", dispatchedTaskRepository.countTasksByQuarter(userId, 4));
//
//        return new QuarterlyTaskStatisticsDTO(userId, quarterlyTasks);
//    }
//
//    @Override
//    public TaskStateStatisticsDTO getTaskStateStatistics(Long userId) {
//        int pendingCount = dispatchedTaskRepository.countTasksByState(userId, 1);
//        int inProgressCount = dispatchedTaskRepository.countTasksByState(userId, 2);
//        int completedCount = dispatchedTaskRepository.countTasksByState(userId, 3);
//
//        return new TaskStateStatisticsDTO(userId, pendingCount, inProgressCount, completedCount);
//    }
//
//    // Mapping DTO field names to Entity field names (Including Common Fields)
//    private String mapDtoToEntityField(String dtoField) {
//        return switch (dtoField) {
//            // DispatchedTask-specific fields
//            case "dispatch_time" -> "dispatchTime";
//            case "due_date" -> "dueDate";
//            case "finished_at" -> "finishedAt";
//            case "dispatched_task_state_id" -> "stateId";
//            case "dispatch_id" -> "dispatch.id";
//
//            // Common fields
//            case "created_at" -> "createdAt";
//            case "created_by" -> "createdBy";
//            case "updated_at" -> "updatedAt";
//            case "updated_by" -> "updatedBy";
//
//            default -> dtoField; // Default to original name if no mapping is needed
//        };
//    }
//}