//package com.fps.svmes.services.impl;
//
//import com.cronutils.descriptor.CronDescriptor;
//import com.cronutils.model.Cron;
//import com.cronutils.model.CronType;
//import com.cronutils.model.definition.CronDefinitionBuilder;
//import com.fps.svmes.dto.dtos.dispatch.*;
//import com.fps.svmes.models.nosql.FormNode;
//import com.fps.svmes.models.sql.production.Product;
//import com.fps.svmes.models.sql.taskSchedule.*;
//import com.fps.svmes.models.sql.user.User;
//import com.fps.svmes.repositories.jpaRepo.dispatch.*;
//import com.fps.svmes.repositories.jpaRepo.maintenance.EquipmentRepository;
//import com.fps.svmes.repositories.jpaRepo.maintenance.MaintenanceWorkOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.production.ProductRepository;
//import com.fps.svmes.repositories.jpaRepo.production.ProductionWorkOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.production.RawMaterialRepository;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
//import com.fps.svmes.services.*;
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.validation.constraints.NotNull;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.modelmapper.ModelMapper;
//import java.time.ZoneOffset;
//import java.time.OffsetDateTime;
//import java.util.*;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import static java.util.stream.Collectors.toList;
//import com.cronutils.parser.CronParser;
//
///**
// * Implementation of the DispatchService interface.
// */
//@Service
//@Transactional
//public class DispatchServiceImpl implements DispatchService {
//
//    @Autowired
//    private DispatchRepository dispatchRepo;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private FormNodeRepository formNodeRepository;
//
//    @Autowired
//    private DispatchedTaskRepository dispatchedTaskRepo;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private RawMaterialRepository rawMaterialRepository;
//
//    @Autowired
//    private ProductionWorkOrderRepository productionWorkOrderRepository;
//
//    @Autowired
//    private EquipmentRepository equipmentRepository;
//
//    @Autowired
//    private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
//
//    @Autowired
//    private InstrumentRepository instrumentRepository;
//
//    @Autowired
//    private SamplingLocationRepository samplingLocationRepository;
//
//    @Autowired
//    private TestSubjectRepository testSubjectRepository;
//
//    @Autowired
//    private TaskScheduleService taskScheduleService;
//
//    @Autowired
//    private DispatchedTaskService dispatchedTaskService;
//
//    @Autowired
//    private FormNodeService formNodeService;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    private static final Logger logger = LoggerFactory.getLogger(DispatchServiceImpl.class);
//
//    // ------------- Dispatch CRUD -----------------------------------------------------------------------
//
//    // Map dispatch request to a dispatch object,
//    @Transactional
//    public Dispatch createDispatch(DispatchDTO request) {
//        Dispatch dispatch = modelMapper.map(request, Dispatch.class);
//
//        // TODO: Not sure if model mapper will map super class fields? if yes this line is redundant
//        dispatch.setCreationDetails(request.getCreatedBy(), 1);
//
//        // Initialize mutable collections
//        List<DispatchForm> mutableDispatchForms = new ArrayList<>();
//        List<DispatchUser> mutableDispatchUsers = new ArrayList<>();
//
//        List<DispatchProduct> mutableDispatchProducts = new ArrayList<>();
//        List<DispatchRawMaterial> mutableDispatchRawMaterials = new ArrayList<>();
//        List<DispatchProductionWorkOrder> mutableDispatchProductionWorkOrders = new ArrayList<>();
//        List<DispatchEquipment> mutableDispatchEquipments = new ArrayList<>();
//        List<DispatchMaintenanceWorkOrder> mutableDispatchMaintenanceWorkOrders = new ArrayList<>();
//
//        List<DispatchInstrument> mutableDispatchInstruments = new ArrayList<>();
//        List<DispatchSamplingLocation> mutableDispatchSamplingLocations = new ArrayList<>();
//        List<DispatchTestSubject> mutableDispatchTestSubjects = new ArrayList<>();
//
//        // Create list of association entities
//        if (request.getFormIds() != null) {
//            mutableDispatchForms.addAll(mapDispatchForms(dispatch, request.getFormIds()));
//        }
//
//        if (request.getUserIds() != null) {
//            mutableDispatchUsers.addAll(mapDispatchUsers(dispatch, request.getUserIds()));
//        }
//
//        if (request.getProductIds() != null) {
//            mutableDispatchProducts.addAll(mapDispatchProducts(dispatch, request.getProductIds()));
//        }
//
//        if (request.getRawMaterialIds() != null) {
//            mutableDispatchRawMaterials.addAll(mapDispatchRawMaterials(dispatch, request.getRawMaterialIds()));
//        }
//
//        if (request.getProductionWorkOrderIds() != null) {
//            mutableDispatchProductionWorkOrders.addAll(mapDispatchProductionWorkOrders(dispatch, request.getProductionWorkOrderIds()));
//        }
//
//        if (request.getEquipmentIds() != null) {
//            mutableDispatchEquipments.addAll(mapDispatchEquipments(dispatch, request.getEquipmentIds()));
//        }
//
//        if (request.getMaintenanceWorkOrderIds() != null) {
//            mutableDispatchMaintenanceWorkOrders.addAll(mapDispatchMaintenanceWorkOrders(dispatch, request.getMaintenanceWorkOrderIds()));
//        }
//
//        if (request.getInstrumentIds() != null) {
//            mutableDispatchInstruments.addAll(mapDispatchInstruments(dispatch, request.getInstrumentIds()));
//        }
//
//        if (request.getSamplingLocationIds() != null) {
//            mutableDispatchSamplingLocations.addAll(mapDispatchSamplingLocations(dispatch, request.getSamplingLocationIds()));
//        }
//
//        if (request.getTestSubjectIds() != null) {
//            mutableDispatchTestSubjects.addAll(mapDispatchTestSubjects(dispatch, request.getTestSubjectIds()));
//        }
//
//        // Check if the required associations (DispatchForms or DispatchUsers) are not empty
//        if (mutableDispatchForms.isEmpty() && mutableDispatchUsers.isEmpty()) {
//            throw new IllegalStateException("Cannot create Dispatch without valid DispatchForms or DispatchUsers");
//        }
//
//        // Set the collections back to the entity
//        dispatch.setDispatchForms(mutableDispatchForms);
//        dispatch.setDispatchUsers(mutableDispatchUsers);
//
//        dispatch.setDispatchProducts(mutableDispatchProducts);
//        dispatch.setDispatchRawMaterials(mutableDispatchRawMaterials);
//        dispatch.setDispatchProductionWorkOrders(mutableDispatchProductionWorkOrders);
//        dispatch.setDispatchEquipments(mutableDispatchEquipments);
//        dispatch.setDispatchMaintenanceWorkOrders(mutableDispatchMaintenanceWorkOrders);
//
//        dispatch.setDispatchInstruments(mutableDispatchInstruments);
//        dispatch.setDispatchSamplingLocations(mutableDispatchSamplingLocations);
//        dispatch.setDispatchTestSubjects(mutableDispatchTestSubjects);
//
//        Dispatch savedDispatch = dispatchRepo.save(dispatch);
//
//        // Insert rows to dispatched task table
//        try {
//            this.initializeDispatch(savedDispatch.getId(), () -> executeDispatch(savedDispatch.getId()));
//
//        } catch (IllegalStateException e) {
//            logger.warn("Dispatch created but not immediately scheduled: {}", e.getMessage());
//        }
//
//        return savedDispatch;
//    }
//
//    @Transactional
//    public Dispatch updateDispatch(Long id, DispatchDTO request) {
//        logger.info("Running updateDispatch for Dispatch ID: {}", id);
//        Dispatch dispatch = dispatchRepo.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        // Set all dispatched tasks by this dispatch to cancel state if in pending state
//        List<DispatchedTask> dispatchedTasks = dispatchedTaskRepo.findByDispatchIdAndStateIdAndStatus(dispatch.getId(), 1, 1);
//        if (!dispatchedTasks.isEmpty()) {
//            // Update state for all fetched tasks in one go
//            dispatchedTasks.forEach(task -> task.setStateId((short) 4));
//
//            // Batch save the updated tasks
//            dispatchedTaskRepo.saveAll(dispatchedTasks);
//        }
//
//        // Update dispatch based on request
//        if (request.getUpdatedBy() != null) {
//            dispatch.setUpdateDetails(request.getUpdatedBy(), 1);
//        }
//
//        if (request.getType() != null) {
//            dispatch.setType(request.getType());
//        }
//
//        if (request.getName() != null) {
//            dispatch.setName(request.getName());
//        }
//
//        if (request.getDescription() != null) {
//            dispatch.setDescription(request.getDescription());
//        }
//
//        if (request.getState() != null) {
//            dispatch.setState(request.getState());
//        }
//
//        if (request.getStartTime() != null) {
//            dispatch.setStartTime(request.getStartTime());
//        }
//
//        if (request.getEndTime() != null) {
//            dispatch.setEndTime(request.getEndTime());
//        }
//
//        if (request.getCronExpression() != null) {
//            dispatch.setCronExpression(request.getCronExpression());
//        }
//
//        if (request.getDispatchLimit() != null) {
//            dispatch.setDispatchLimit(request.getDispatchLimit());
//        }
//
//        if (request.getCustomTime() != null) {
//            dispatch.setCustomTime(request.getCustomTime());
//        }
//
//        if (request.getExecutedCount() != null) {
//            dispatch.setExecutedCount(request.getExecutedCount());
//        }
//
//        if (request.getDueDateOffsetMinute() != null) {
//            dispatch.setDueDateOffsetMinute(request.getDueDateOffsetMinute());
//        }
//
//        // Clear and update associations with proper handling
//        updateDispatchUsers(dispatch, request.getUserIds());
//        updateDispatchForms(dispatch, request.getFormIds());
//        updateDispatchProducts(dispatch, request.getProductIds());
//        updateDispatchRawMaterials(dispatch, request.getRawMaterialIds());
//        updateDispatchProductionWorkOrders(dispatch, request.getProductionWorkOrderIds());
//        updateDispatchEquipments(dispatch, request.getEquipmentIds());
//        updateDispatchMaintenanceWorkOrders(dispatch, request.getMaintenanceWorkOrderIds());
//        updateDispatchSamplingLocations(dispatch, request.getSamplingLocationIds());
//        updateDispatchInstruments(dispatch, request.getInstrumentIds());
//        updateDispatchTestSubjects(dispatch, request.getTestSubjectIds());
//
//        Dispatch updatedDispatch = dispatchRepo.save(dispatch);
//
//        if (updatedDispatch.getStatus() == 1) {
//            taskScheduleService.removeAllTasks(dispatch.getId());
//            initializeDispatch(id, () -> executeDispatch(updatedDispatch.getId()));
//        }
//
//        return updatedDispatch;
//    }
//
//    @Transactional
//    public void deleteDispatch(Long id, Integer userId) {
//        logger.info("Running deleteDispatch for Dispatch ID: {}", id);
//        Dispatch dispatch = dispatchRepo.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        // Cancel any scheduled task of this dispatch if scheduled
//        boolean taskCancelled = taskScheduleService.removeAllTasks(id);
//        if (taskCancelled) {
//            logger.info("Cancelled all scheduled task for Dispatch ID: {}", id);
//        }
//
//        // Set all dispatched tasks under this order that are in pending mode (state 1) to canceled mode (state 4)
//        List<DispatchedTask> dispatchTasks = dispatchedTaskRepo.findByDispatchIdAndStateIdAndStatus(dispatch.getId(), 1, 1);
//
//        if (!dispatchTasks.isEmpty()) {
//            // Update state for all fetched tasks in one go
//            dispatchTasks.forEach(task -> task.setStateId((short) 4));
//
//            // Batch save the updated tasks
//            dispatchedTaskRepo.saveAll(dispatchTasks);
//        }
//
//        dispatch.setState(DispatchState.Inactive.getState());
//        dispatch.setUpdateDetails(userId, 0);
//
//        // Update the status of related personnel and forms
//        if (dispatch.getDispatchUsers() != null) { dispatch.getDispatchUsers().forEach(dispatchUser -> dispatchUser.setUpdateDetails(userId, 0));
//        }
//        if (dispatch.getDispatchForms() != null) {
//            dispatch.getDispatchForms().forEach(dispatchForm -> dispatchForm.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchProducts() != null) {
//            dispatch.getDispatchProducts().forEach(dispatchProduct -> dispatchProduct.setUpdateDetails(userId, 0));
//        }
//        if (dispatch.getDispatchRawMaterials() != null) {
//            dispatch.getDispatchRawMaterials().forEach(dispatchRawMaterial -> dispatchRawMaterial.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchProductionWorkOrders() != null) {
//            dispatch.getDispatchProductionWorkOrders().forEach(dispatchProductionWorkOrder -> dispatchProductionWorkOrder.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchEquipments() != null) {
//            dispatch.getDispatchEquipments().forEach(dispatchEquipment -> dispatchEquipment.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchMaintenanceWorkOrders() != null) {
//            dispatch.getDispatchMaintenanceWorkOrders().forEach(dispatchMaintenanceWorkOrder -> dispatchMaintenanceWorkOrder.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchInstruments() != null) {
//            dispatch.getDispatchInstruments().forEach(dispatchInstrument -> dispatchInstrument.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchSamplingLocations() != null) {
//            dispatch.getDispatchSamplingLocations().forEach(dispatchSamplingLocation -> dispatchSamplingLocation.setUpdateDetails(userId,0));
//        }
//        if (dispatch.getDispatchTestSubjects() != null) {
//            dispatch.getDispatchTestSubjects().forEach(dispatchTestSubject -> dispatchTestSubject.setUpdateDetails(userId,0));
//        }
//
//        dispatchRepo.save(dispatch);
//        logger.warn("Soft-deleted Dispatch ID: {}", id);
//    }
//
//    @Transactional(readOnly = true)
//    public DispatchDTO getDispatch(Long id) {
//        Optional<Dispatch> dispatch = dispatchRepo.findByIdAndStatus(id, 1);
//        if (dispatch.isPresent()){
//            return convertToDispatchDTO(dispatch.get());
//        } else {
//            throw new EntityNotFoundException("Dispatch with ID " + id + " not found");
//        }
//    }
//
//    @Transactional(readOnly = true)
//    public List<DispatchDTO> getAllDispatches() {
//        return dispatchRepo.findAll()
//                .stream()
//                .filter(dispatch -> dispatch.getStatus() == 1)
//                .map(this::convertToDispatchDTO)
//                .collect(toList());
//    }
//
//    @Transactional
//    public DispatchDTO getDispatchByDispatchedTaskId(Long dispatchedTaskId) {
//        // Fetch the dispatched task using its repository (ensure you have injected DispatchedTaskRepository)
//        DispatchedTask dispatchedTask = dispatchedTaskRepo.findById(dispatchedTaskId)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatched Task not found with ID: " + dispatchedTaskId));
//
//        // Ensure the dispatched task has an associated dispatch
//        if (dispatchedTask.getDispatch() == null) {
//            throw new EntityNotFoundException("No dispatch found for dispatched task ID: " + dispatchedTaskId);
//        }
//
//        // Use your existing conversion method to create a DTO for the Dispatch
//        return convertToDispatchDTO(dispatchedTask.getDispatch());
//    }
//
//
//    // ------------- SCHEDULING LOGIC -----------------------------------------------------------------------
//
//    // Runs on server start up
//    @EventListener(ApplicationReadyEvent.class)
//    public void initializeSchedules() {
//        logger.info("Initializing scheduled tasks...");
//        scheduleDispatches();
//        logger.info("Done initializing scheduling dispatches...");
//
//    }
//
//    // Go through all existing dispatch to initialize scheduling
//    @Override
//    public void scheduleDispatches() {
//        dispatchRepo.findByStatus(1).stream()
//                .filter(dispatch -> "regular".equals(dispatch.getType()) &&
//                        dispatch.getStatus() == 1 &&
//                        !taskScheduleService.isScheduled(dispatch.getId()))
//                .forEach(dispatch -> {
//                    try {
//                        initializeDispatch(dispatch.getId(), () -> executeDispatch(dispatch.getId()));
//                    } catch (Exception e) {
//                        logger.error("Failed to schedule task for Dispatch ID: {}", dispatch.getId(), e);
//                    }
//                });
//    }
//
//    // Runs every 10 minutes (600000 ms)
//    @Scheduled(fixedRate = 600000)
//    public void periodicCleanupExpiredTasks() {
//        dispatchRepo.findByStatus(1).stream()
//                .filter(dispatch -> "regular".equals(dispatch.getType()) &&
//                        dispatch.getState().equals(DispatchState.Active.getState()))
//                .forEach(dispatch -> {
//                    if (dispatch.getEndTime().isBefore(OffsetDateTime.now())) {
//                        logger.info("Dispatch ID {} has expired. Canceling task...", dispatch.getId());
//                        cancelDispatchTask(dispatch.getId());
//                    }
//                });
//    }
//
//    // called by create dispatch, update dispatch, initialization upon server restart , schedule by id
//    // This function setup task(CRON, FUTURE, CANCEL) based on the start/end time of dispatch
//    public void initializeDispatch(Long dispatchId, Runnable task) {
//        Dispatch dispatch = dispatchRepo.findById(dispatchId)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        OffsetDateTime now = OffsetDateTime.now();
//
//        // Setup schedule type dispatch (Dispatch during start to end time based on cron expression, dispatch limit)
//        if (dispatch.getType().equals("regular")) {
//
//            // Dispatch is already scheduled
//            if (taskScheduleService.isScheduled(dispatchId)){
//                logger.info("Dispatch ID {} is already scheduled.", dispatchId);
//                return;
//            }
//
//            if (dispatch.getEndTime().isBefore(now)) {
//                logger.info("Dispatch ID {} has an end time in the past. Skipping scheduling.", dispatchId);
//                // set dispatch to be inactive
//                if (dispatch.getState() == DispatchState.Active.getState()) {
//                    dispatch.setState(DispatchState.Expired.getState());
//                    dispatch.setUpdatedAt(OffsetDateTime.now());
//                    dispatchRepo.save(dispatch);
//                }
//                return;
//            }
//
//            if (dispatch.getStartTime().isAfter(now)) {
//                taskScheduleService.scheduleFutureDispatch(dispatch, task);
//            } else {
//                taskScheduleService.scheduleDispatch(dispatch, task);
//            }
//        } else {
//            // Set up custom type dispatch (Only dispatch once at custom time. start time, end time, dispatch limit,
//            // and cron expression are all ignored)
//            taskScheduleService.scheduleCustomDispatch(dispatch, task);
//        }
//    }
//
//    // This function updates isActive where needed, and insert rows to dispatched task table
//    @Transactional
//    @Override
//    public void executeDispatch(Long dispatchId) {
//        Dispatch dispatch = dispatchRepo.findById(dispatchId)
//                .orElseThrow(() -> new EntityNotFoundException("Execute Dispatch Failed: Dispatch not found with ID: " + dispatchId));
//
//        // active (set as default when creating/updating by hardcoded 1 in frontend, when a task is scheduled for a dispatch)
//        // inactive (cancel task or delete dispatch or default state for custom type dispatch, cancel task in task scheduler)
//        // expired (set during initialization based on now vs endtime, does not scheduled so wont trigger executeDispatch)
//        // exhausted (set in executeDispatch)
//        // paused (set when calling pause)
//        // invalid (set when executeDispatch gone wrong)
//
//        try {
//            // Load dispatchForms separately
//            Dispatch dispatchWithForms = dispatchRepo.findWithFormsById(dispatchId)
//                    .orElseThrow(() -> new EntityNotFoundException("Dispatch forms not found"));
//            dispatch.setDispatchForms(dispatchWithForms.getDispatchForms());
//
//            // Load dispatchUsers separately
//            Dispatch dispatchWithUsers = dispatchRepo.findWithUsersById(dispatchId)
//                    .orElseThrow(() -> new EntityNotFoundException("Dispatch users not found"));
//            dispatch.setDispatchUsers(dispatchWithUsers.getDispatchUsers());
//
//            // Ensure collections are fully initialized before processing
//            dispatch.getDispatchForms().size();
//            dispatch.getDispatchUsers().size();
//
//            // Update dispatchForms based on referenced entity status
//            dispatch.getDispatchForms().forEach(dispatchForm -> {
//                String qcFormTreeNodeId = dispatchForm.getQcFormTreeNodeId();
//                Optional<FormNode> formNode = formNodeService.getNodeByIdOrUuid(qcFormTreeNodeId);
//                dispatchForm.setStatus(formNode.isPresent() ? 1 : 0);
//            });
//
//            // Update dispatchUsers based on referenced user status
//            dispatch.getDispatchUsers().forEach(dispatchUser -> {
//                Optional<User> user = userRepository.findById(dispatchUser.getUser().getId());
//                dispatchUser.setStatus(user.isPresent() && user.get().getStatus() == 1 ? 1 : 0);
//            });
//
//            // Check and cancel dispatch if it has a execution limit
//            if ((Objects.equals(dispatch.getType(), "regular")) && (dispatch.getDispatchLimit() != -1) && (dispatch.getExecutedCount() >= dispatch.getDispatchLimit())) {
//                if(taskScheduleService.isScheduled(dispatch.getId())) {
//                    taskScheduleService.removeAllTasks(dispatch.getId());
//                }
//                dispatch.setState(DispatchState.Exhausted.getState());
//                dispatchRepo.save(dispatch);
//                return;
//            }
//
//            // insert dispatched task rows and increase executed count
//            processDispatch(dispatch);
//            dispatch.setUpdatedAt(OffsetDateTime.now());
//            dispatchRepo.save(dispatch);
//        } catch (Exception e) {
//            dispatch.setState(DispatchState.Invalid.getState());
//            taskScheduleService.removeAllTasks(dispatch.getId());
//            dispatchRepo.save(dispatch);
//            logger.error("Error executing Dispatch ID: {}, set dispatch to invalid", dispatchId, e);
//        }
//    }
//
//    // This function cancels all tasks of a dispatch
//    @Override
//    public void cancelDispatchTask(Long dispatchId) {
//        Dispatch dispatch = dispatchRepo.findById(dispatchId)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        if (taskScheduleService.removeAllTasks(dispatchId)) {
//            // Update status after cancellation
//            dispatch.setUpdatedAt(OffsetDateTime.now());
//            dispatch.setState(DispatchState.Inactive.getState());
//            dispatchRepo.save(dispatch);
//        } else {
//            logger.info("No task was scheduled for this dispatch ID.");
//        }
//    }
//
//    // This function cancels all tasks of a dispatch
//    @Override
//    public void pauseDispatch(Long dispatchId, Integer userId) {
//        Dispatch dispatch = dispatchRepo.findById(dispatchId)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        if (taskScheduleService.removeTask(dispatchId, TaskType.CRON)) {
//            // Update status after cancellation
//            dispatch.setUpdateDetails(userId, 1);
//            dispatch.setState(DispatchState.Paused.getState());
//            dispatchRepo.save(dispatch);
//        } else {
//            logger.info("No task was scheduled for this dispatch ID.");
//        }
//    }
//
//    public void resumeDispatch(Long dispatchId, Integer userId) {
//        Dispatch dispatch = dispatchRepo.findById(dispatchId)
//                .orElseThrow(() -> new EntityNotFoundException("Dispatch not found"));
//
//        taskScheduleService.setupCronTask(dispatch,() -> executeDispatch(dispatch.getId()));
//        dispatch.setUpdateDetails(userId, 1);
//        dispatch.setState(DispatchState.Active.getState());
//        dispatchRepo.save(dispatch);
//    }
//
//    // // ------------- Helper Function  -----------------------------------------------------------------------
//
//    private boolean hasReachedDispatchLimit(Dispatch dispatch) {
//        return dispatch.getDispatchLimit() != -1 && dispatch.getExecutedCount() >= dispatch.getDispatchLimit();
//    }
//
//    private void processDispatch(Dispatch dispatch) {
//        createTasksForDispatch(dispatch);
//        dispatch.setExecutedCount(dispatch.getExecutedCount() + 1); // Increment executed count
//    }
//
//    public DispatchDTO convertToDispatchDTO(Dispatch dispatch) {
//        DispatchDTO dto = modelMapper.map(dispatch, DispatchDTO.class);
//
//        // Fetch all form nodes once and filter dispatch forms
//        if (dispatch.getDispatchForms() != null) {
//            dto.setFormIds(dispatch.getDispatchForms().stream()
//                    .filter(dispatchForm -> dispatchForm.getStatus() == 1 &&
//                            formNodeService.getNodeByIdOrUuid(dispatchForm.getQcFormTreeNodeId()).isPresent())
//                    .map(DispatchForm::getQcFormTreeNodeId)
//                    .toList());
//        }
//
//        // Convert dispatchUsers to userIds
//        if (dispatch.getDispatchUsers() != null) {
//            dto.setUserIds(dispatch.getDispatchUsers()
//                    .stream()
//                    .filter(dispatchUser -> dispatchUser.getStatus() == 1)
//                    .map(DispatchUser::getUser)
//                    .map(User::getId)
//                    .toList());
//        }
//
//        // Convert dispatchUsers to userIds
//        if (dispatch.getDispatchUsers() != null) {
//            dto.setUserIds(dispatch.getDispatchUsers().stream()
//                    .filter(dispatchUser -> dispatchUser.getStatus() == 1 &&
//                            dispatchUser.getUser().getStatus() == 1)
//                    .map(dispatchUser -> dispatchUser.getUser().getId())
//                    .toList());
//        }
//
//        // Convert system association rows to respective ids.
//        dto.setProductIds(fetchValidIds(dispatch.getDispatchProducts(),
//                dp -> dp.getProduct().getId(),
//                dp -> dp.getStatus() == 1 && dp.getProduct().getStatus() == 1));
//
//        dto.setRawMaterialIds(fetchValidIds(dispatch.getDispatchRawMaterials(),
//                dr -> dr.getRawMaterial().getId(),
//                dr -> dr.getStatus() == 1 && dr.getRawMaterial().getStatus() == 1));
//
//        dto.setProductionWorkOrderIds(fetchValidIds(dispatch.getDispatchProductionWorkOrders(),
//                pwo -> pwo.getProductionWorkOrder().getId(),
//                pwo -> pwo.getStatus() == 1 && pwo.getProductionWorkOrder().getStatus() == 1));
//
//        dto.setEquipmentIds(fetchValidIds(dispatch.getDispatchEquipments(),
//                de -> de.getEquipment().getId(),
//                de -> de.getStatus() == 1 && de.getEquipment().getStatus() == 1));
//
//        dto.setMaintenanceWorkOrderIds(fetchValidIds(dispatch.getDispatchMaintenanceWorkOrders(),
//                dmwo -> dmwo.getMaintenanceWorkOrder().getId(),
//                dmwo -> dmwo.getStatus() == 1 && dmwo.getMaintenanceWorkOrder().getStatus() == 1));
//
//        dto.setInstrumentIds(fetchValidIds(dispatch.getDispatchInstruments(),
//                di-> di.getInstrument().getId(),
//                di-> di.getStatus() == 1 && di.getInstrument().getStatus() == 1));
//
//        dto.setSamplingLocationIds(fetchValidIds(dispatch.getDispatchSamplingLocations(),
//                di-> di.getSamplingLocation().getId(),
//                di-> di.getStatus() == 1 && di.getSamplingLocation().getStatus() == 1));
//
//        dto.setTestSubjectIds(fetchValidIds(dispatch.getDispatchTestSubjects(),
//                di-> di.getTestSubject().getId(),
//                di-> di.getStatus() == 1 && di.getTestSubject().getStatus() == 1));
//
//        return dto;
//    }
//
//    private <T, R> List<R> fetchValidIds(List<T> entities, Function<T, R> idMapper, Predicate<T> filter) {
//        if (entities == null || entities.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return entities.stream()
//                .filter(filter)
//                .map(idMapper)
//                .toList();
//    }
//
//    private void createTasksForDispatch(Dispatch dispatch) {
//        OffsetDateTime dispatchTime = OffsetDateTime.now(ZoneOffset.UTC);
//        OffsetDateTime dueDate = calculateDueDate(dispatchTime, dispatch.getDueDateOffsetMinute());
//
//        // Prepare a DispatchedTaskDTO template based on dispatch
//        DispatchedTaskDTO taskDTO = new DispatchedTaskDTO();
//        taskDTO.setDispatchId(dispatch.getId());
//        taskDTO.setDispatchTime(dispatchTime);
//        taskDTO.setName(dispatch.getName());
//        taskDTO.setDescription(dispatch.getDescription());
//        taskDTO.setDueDate(dueDate);
//        taskDTO.setIsOverdue(false);
//        taskDTO.setStateId((short) 1); // Default state ID
//        taskDTO.setCreationDetails(dispatch.getCreatedBy(), 1);
//        taskDTO.setNotes(dispatch.getDescription());
//
//        // Filter DispatchForms with status = 1
//        List<DispatchForm> activeForms = dispatch.getDispatchForms().stream()
//                .filter(dispatchForm -> dispatchForm.getStatus() == 1)
//                .toList();
//
//        // Filter DispatchUsers with status = 1
//        List<Integer> activeUserIds = dispatch.getDispatchUsers().stream()
//                .filter(dispatchUser -> dispatchUser.getStatus() == 1)
//                .map(user -> user.getUser().getId())
//                .toList();
//
//        // Loop through active forms and create tasks for active users
//        for (DispatchForm form : activeForms) {
//            taskDTO.setQcFormTreeNodeId(form.getQcFormTreeNodeId());
//
//            // Use the service to insert tasks
//            dispatchedTaskService.insertDispatchedTasks(taskDTO, activeUserIds);
//        }
//
//        logger.info("Executed Dispatch ID: {}, Created {} tasks.", dispatch.getId(), dispatch.getDispatchForms().size());
//    }
//
//    private OffsetDateTime calculateDueDate(OffsetDateTime dispatchTime, int dueDateOffsetMinute) {
//        return dispatchTime.plusMinutes(dueDateOffsetMinute);
//    }
//
//    private List<DispatchForm> mapDispatchForms(Dispatch dispatch, List<String> formIds) {
//        return formIds.stream()
//                .map(formTreeNodeId ->
//                        {
//                            Optional<FormNode> formNode = formNodeService.getNodeByIdOrUuid(formTreeNodeId);
//
//                            if (formNode.isPresent()) {
//                                DispatchForm df = new DispatchForm();
//                                df.setDispatch(dispatch);
//                                df.setQcFormTreeNodeId(formTreeNodeId);
//                                df.setCreationDetails(dispatch.getCreatedBy(), 1);
//                                return df;
//                            } else {
//                                return null;
//                            }
//                        })
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchUser> mapDispatchUsers(Dispatch dispatch, @NotNull(message = "List of user IDs cannot be null") List<Integer> ids) {
//        return ids.stream()
//                .map(userId -> userRepository.findById(userId)
//                        .filter(user -> user.getStatus() == 1) // Ensure user status is active (status == 1)
//                        .map(user -> {
//                            DispatchUser du = new DispatchUser();
//                            du.setDispatch(dispatch);
//                            du.setUser(user);
//                            du.setCreationDetails(dispatch.getCreatedBy(), 1);
//                            return du;
//                        })
//                        .orElse(null)) // Return null if user doesn't exist or is inactive
//                .filter(Objects::nonNull) // Remove null values from the resulting list
//                .toList();
//    }
//
//    private List<DispatchProduct> mapDispatchProducts(Dispatch dispatch, List<Integer> ids) {
//        return ids.stream()
//                .map(product_id -> productRepository.findById(product_id)
//                            .filter(product -> product.getStatus() == 1)
//                            .map(product -> {
//                                DispatchProduct dp = new DispatchProduct();
//                                dp.setDispatch(dispatch);
//                                dp.setProduct(product);
//                                dp.setCreationDetails(dispatch.getCreatedBy(),1);
//                                return dp;
//                            })
//                            .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchRawMaterial> mapDispatchRawMaterials(Dispatch dispatch, List<Integer> ids) {
//        return ids.stream()
//                .map(id -> rawMaterialRepository.findById(id)
//                        .filter(rawMaterial -> rawMaterial.getStatus() == 1) // Check if status is active
//                        .map(rawMaterial -> {
//                            DispatchRawMaterial dp = new DispatchRawMaterial();
//                            dp.setDispatch(dispatch);
//                            dp.setRawMaterial(rawMaterial);
//                            dp.setCreationDetails(dispatch.getCreatedBy(),1); // Default active status
//                            return dp;
//                        })
//                        .orElse(null)) // Return null if raw material does not exist or is inactive
//                .filter(Objects::nonNull) // Filter out null values
//                .toList();
//    }
//
//    private List<DispatchProductionWorkOrder> mapDispatchProductionWorkOrders(Dispatch dispatch, List<Integer> ids) {
//        return ids.stream()
//                .map(id -> productionWorkOrderRepository.findById(id)
//                        .filter(workOrder -> workOrder.getStatus() == 1) // Check if status is active
//                        .map(workOrder -> {
//                            DispatchProductionWorkOrder dp = new DispatchProductionWorkOrder();
//                            dp.setDispatch(dispatch);
//                            dp.setProductionWorkOrder(workOrder);
//                            dp.setCreationDetails(dispatch.getCreatedBy(), 1); // Default active status
//                            return dp;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchEquipment> mapDispatchEquipments(Dispatch dispatch, List<Short> ids) {
//        return ids.stream()
//                .map(id -> equipmentRepository.findById(id)
//                        .filter(equipment -> equipment.getStatus() == 1) // Check if status is active
//                        .map(equipment -> {
//                            DispatchEquipment dp = new DispatchEquipment();
//                            dp.setDispatch(dispatch);
//                            dp.setEquipment(equipment);
//                            dp.setCreationDetails(dispatch.getCreatedBy(), 1); // Default active status
//                            return dp;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchMaintenanceWorkOrder> mapDispatchMaintenanceWorkOrders(Dispatch dispatch, List<Integer> ids) {
//        return ids.stream()
//                .map(id -> maintenanceWorkOrderRepository.findById(id)
//                        .filter(workOrder -> workOrder.getStatus() == 1) // Check if status is active
//                        .map(workOrder -> {
//                            DispatchMaintenanceWorkOrder dp = new DispatchMaintenanceWorkOrder();
//                            dp.setDispatch(dispatch);
//                            dp.setMaintenanceWorkOrder(workOrder);
//                            dp.setCreationDetails(dispatch.getCreatedBy(),1); // Default active status
//                            return dp;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchInstrument> mapDispatchInstruments(Dispatch dispatch, List<Long> ids) {
//        return ids.stream()
//                .map(id -> instrumentRepository.findById(id) // Assuming you have an instrument repository
//                        .filter(instrument -> instrument.getStatus() == 1) // Check if status is active
//                        .map(instrument -> {
//                            DispatchInstrument di = new DispatchInstrument();
//                            di.setDispatch(dispatch);
//                            di.setInstrument(instrument); // Assuming there's a method to set the instrument object
//                            di.setCreationDetails(dispatch.getCreatedBy(), 1);
//                            return di;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchSamplingLocation> mapDispatchSamplingLocations(Dispatch dispatch, List<Long> ids) {
//        return ids.stream()
//                .map(id -> samplingLocationRepository.findById(id) // Assuming you have an instrument repository
//                        .filter(samplingLocation -> samplingLocation.getStatus() == 1) // Check if status is active
//                        .map(samplingLocation -> {
//                            DispatchSamplingLocation ds = new DispatchSamplingLocation();
//                            ds.setDispatch(dispatch);
//                            ds.setSamplingLocation(samplingLocation); // Assuming there's a method to set the instrument object
//                            ds.setCreationDetails(dispatch.getCreatedBy(), 1);
//                            return ds;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private List<DispatchTestSubject> mapDispatchTestSubjects(Dispatch dispatch, List<Long> ids) {
//        return ids.stream()
//                .map(id -> testSubjectRepository.findById(id) // Assuming you have an instrument repository
//                        .filter(testSubject -> testSubject.getStatus() == 1) // Check if status is active
//                        .map(testSubject -> {
//                            DispatchTestSubject dt = new DispatchTestSubject();
//                            dt.setDispatch(dispatch);
//                            dt.setTestSubject(testSubject); // Assuming there's a method to set the instrument object
//                            dt.setCreationDetails(dispatch.getCreatedBy(), 1);
//                            return dt;
//                        })
//                        .orElse(null))
//                .filter(Objects::nonNull)
//                .toList();
//    }
//
//    private void updateDispatchForms(Dispatch dispatch, List<String> formIds) {
//        if (formIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchForms().forEach(dispatchForm -> dispatchForm.setStatus(0));
//
//        // Reactivate or add new rows
//        formIds.forEach(id -> {
//            DispatchForm existing = dispatch.getDispatchForms().stream()
//                    .filter(dispatchForm -> dispatchForm.getQcFormTreeNodeId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(), 1); // Default active status
//            } else {
//                // Add new row
//                DispatchForm newDispatchForm = new DispatchForm();
//                newDispatchForm.setDispatch(dispatch);
//                newDispatchForm.setQcFormTreeNodeId(id);
//                newDispatchForm.setCreationDetails(dispatch.getUpdatedBy(), 1); // Default active status
//                dispatch.getDispatchForms().add(newDispatchForm);
//            }
//        });}
//
//    private void updateDispatchUsers(Dispatch dispatch, List<Integer> userIds) {
//        if (userIds == null) return;
//
//        // Get existing users with status = 1
//        List<Integer> existingUsers = dispatch.getDispatchUsers().stream()
//                .filter(dispatchUser -> dispatchUser.getStatus() == 1)
//                .map(DispatchUser::getUser)
//                .map(User::getId)
//                .toList();
//
//        // Reactivate or add new rows for the provided user IDs
//        userIds.forEach(id -> {
//            DispatchUser existing = dispatch.getDispatchUsers().stream()
//                    .filter(dispatchUser -> dispatchUser.getUser().getId()
//                            .equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                existing.setUpdateDetails(dispatch.getUpdatedBy(), 1);
//            } else {
//                // Add new row if not already associated
//                User user = userRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
//                DispatchUser du = new DispatchUser();
//                du.setDispatch(dispatch);
//                du.setUser(user);
//                du.setCreationDetails(dispatch.getUpdatedBy(), 1);
//                dispatch.getDispatchUsers().add(du);
//            }
//        });
//
//        // Deactivate rows that are no longer associated
//        dispatch.getDispatchUsers().stream()
//                .filter(user -> !userIds.contains(user.getUser().getId()))
//                .forEach(user -> user.setStatus(0));
//    }
//
//    private void updateDispatchProducts(Dispatch dispatch, List<Integer> productIds) {
//        if (productIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchProducts().forEach(dp -> dp.setStatus(0));
//
//        // Reactivate or add new rows
//        productIds.forEach(id -> {
//            DispatchProduct existing = dispatch.getDispatchProducts().stream()
//                    .filter(dp -> dp.getProduct().getId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(), 1);
//            } else {
//                // Add new row
//                Product product = productRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
//                DispatchProduct dp = new DispatchProduct();
//                dp.setDispatch(dispatch);
//                dp.setProduct(product);
//                dp.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchProducts().add(dp);
//            }
//        });
//    }
//
//    private void updateDispatchRawMaterials(Dispatch dispatch, List<Integer> rawMaterialIds) {
//        if (rawMaterialIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchRawMaterials().forEach(drm -> drm.setStatus(0));
//
//        // Reactivate or add new rows
//        rawMaterialIds.forEach(id -> {
//            DispatchRawMaterial existing = dispatch.getDispatchRawMaterials().stream()
//                    .filter(drm -> drm.getRawMaterial().getId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(),1);
//            } else {
//                // Add new row
//                DispatchRawMaterial drm = new DispatchRawMaterial();
//                drm.setDispatch(dispatch);
//                drm.setRawMaterial(rawMaterialRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Raw Material not found: " + id)));
//                drm.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchRawMaterials().add(drm);
//            }
//        });
//    }
//
//    private void updateDispatchProductionWorkOrders(Dispatch dispatch, List<Integer> productionWorkOrderIds) {
//        if (productionWorkOrderIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchProductionWorkOrders().forEach(dpwo -> dpwo.setStatus(0));
//
//        // Reactivate or add new rows
//        productionWorkOrderIds.forEach(id -> {
//            DispatchProductionWorkOrder existing = dispatch.getDispatchProductionWorkOrders().stream()
//                    .filter(dpwo -> dpwo.getProductionWorkOrder().getId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(),1);
//            } else {
//                // Add new row
//                DispatchProductionWorkOrder dpwo = new DispatchProductionWorkOrder();
//                dpwo.setDispatch(dispatch);
//                dpwo.setProductionWorkOrder(productionWorkOrderRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Production Work Order not found: " + id)));
//                dpwo.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchProductionWorkOrders().add(dpwo);
//            }
//        });
//    }
//
//    private void updateDispatchEquipments(Dispatch dispatch, List<Short> equipmentIds) {
//        if (equipmentIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchEquipments().forEach(de -> de.setStatus(0));
//
//        // Reactivate or add new rows
//        equipmentIds.forEach(id -> {
//            DispatchEquipment existing = dispatch.getDispatchEquipments().stream()
//                    .filter(de -> de.getEquipment().getId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(), 1);
//            } else {
//                // Add new row
//                DispatchEquipment de = new DispatchEquipment();
//                de.setDispatch(dispatch);
//                de.setEquipment(equipmentRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Equipment not found: " + id)));
//                de.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchEquipments().add(de);
//            }
//        });
//    }
//
//    private void updateDispatchMaintenanceWorkOrders(Dispatch dispatch, List<Integer> maintenanceWorkOrderIds) {
//        if (maintenanceWorkOrderIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchMaintenanceWorkOrders().forEach(dmwo -> dmwo.setStatus(0));
//
//        // Reactivate or add new rows
//        maintenanceWorkOrderIds.forEach(id -> {
//            DispatchMaintenanceWorkOrder existing = dispatch.getDispatchMaintenanceWorkOrders().stream()
//                    .filter(dmwo -> dmwo.getMaintenanceWorkOrder().getId().equals(id))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(), 1);
//            } else {
//                // Add new row
//                DispatchMaintenanceWorkOrder dmwo = new DispatchMaintenanceWorkOrder();
//                dmwo.setDispatch(dispatch);
//                dmwo.setMaintenanceWorkOrder(maintenanceWorkOrderRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Maintenance Work Order not found: " + id)));
//                dmwo.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchMaintenanceWorkOrders().add(dmwo);
//            }
//        });
//    }
//
//    private void updateDispatchSamplingLocations(Dispatch dispatch, List<Long> samplingLocationIds) {
//        if (samplingLocationIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchSamplingLocations().forEach(ds -> ds.setStatus(0));
//
//        // Reactivate or add new rows
//        samplingLocationIds.forEach(id -> {
//            DispatchSamplingLocation existing = dispatch.getDispatchSamplingLocations().stream()
//                    .filter(ds -> id.equals(ds.getSamplingLocation().getId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(),1);
//            } else {
//                // Add new row
//                DispatchSamplingLocation ds = new DispatchSamplingLocation();
//                SamplingLocation samplingLocation= samplingLocationRepository.findById(id)
//                        .orElseThrow(() -> new EntityNotFoundException("Sampling location not found with ID: " + id));
//                ds.setDispatch(dispatch);
//                ds.setSamplingLocation(samplingLocation);
//                ds.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchSamplingLocations().add(ds);
//            }
//        });
//    }
//
//    private void updateDispatchInstruments(Dispatch dispatch, List<Long> instrumentIds) {
//        if (instrumentIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchInstruments().forEach(di -> di.setStatus(0));
//
//        // Reactivate or add new rows
//        instrumentIds.forEach(id -> {
//            DispatchInstrument existing = dispatch.getDispatchInstruments().stream()
//                    .filter(di -> id.equals(di.getInstrument().getId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(),1);
//            } else {
//                // Add new row
//                DispatchInstrument di = new DispatchInstrument();
//                Instrument instrument = instrumentRepository.findById(id).orElseThrow(
//                        () -> new EntityNotFoundException("instrument not found with ID: " + id));
//                di.setDispatch(dispatch);
//                di.setInstrument(instrument);
//                di.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchInstruments().add(di);
//            }
//        });
//    }
//
//    private void updateDispatchTestSubjects(Dispatch dispatch, List<Long> testSubjectIds) {
//        if (testSubjectIds == null) return;
//
//        // Mark all existing rows as inactive
//        dispatch.getDispatchTestSubjects().forEach(dt -> dt.setStatus(0));
//
//        // Reactivate or add new rows
//        testSubjectIds.forEach(id -> {
//            DispatchTestSubject existing = dispatch.getDispatchTestSubjects().stream()
//                    .filter(dt -> id.equals(dt.getTestSubject().getId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existing != null) {
//                // Reactivate if already exists
//                existing.setUpdateDetails(dispatch.getUpdatedBy(),1);
//            } else {
//                // Add new row
//                DispatchTestSubject dt = new DispatchTestSubject();
//                TestSubject testSubject = testSubjectRepository.findById(id).orElseThrow(
//                        () -> new EntityNotFoundException("test subject not found with ID: " + id));
//                dt.setDispatch(dispatch);
//                dt.setTestSubject(testSubject);
//                dt.setCreationDetails(dispatch.getUpdatedBy(),1);
//                dispatch.getDispatchTestSubjects().add(dt);
//            }
//        });
//    }
//
//    public String parseSpringCronToChinese(String cronExpression) {
//
////        return CronToChineseConverter.convertToChinese(cronExpression);
//        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING53));
//        Cron cron = parser.parse(cronExpression);
//        CronDescriptor descriptor = CronDescriptor.instance(Locale.ENGLISH);
//        return descriptor.describe(cron);
//    }
//}