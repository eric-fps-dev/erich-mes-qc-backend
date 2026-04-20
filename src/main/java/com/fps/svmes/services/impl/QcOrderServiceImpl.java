//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.dispatch.DispatchDTO;
//import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
//import com.fps.svmes.models.sql.taskSchedule.*;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchRepository;
//import com.fps.svmes.repositories.jpaRepo.dispatch.DispatchedTaskRepository;
//import com.fps.svmes.repositories.jpaRepo.dispatch.QcOrderDispatchRepository;
//import com.fps.svmes.repositories.jpaRepo.dispatch.QcOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.maintenance.EquipmentRepository;
//import com.fps.svmes.repositories.jpaRepo.maintenance.MaintenanceWorkOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.production.ProductRepository;
//import com.fps.svmes.repositories.jpaRepo.production.ProductionWorkOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.production.RawMaterialRepository;
//import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
//import com.fps.svmes.services.DispatchService;
//import com.fps.svmes.services.QcOrderService;
//import jakarta.persistence.EntityNotFoundException;
//import org.modelmapper.ModelMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//public class QcOrderServiceImpl implements QcOrderService {
//
//    @Autowired private QcOrderRepository qcOrderRepo;
//    @Autowired private QcOrderDispatchRepository qcOrderDispatchRepo;
//    @Autowired private DispatchRepository dispatchRepo;
//    @Autowired private DispatchedTaskRepository dispatchedTaskRepository;
//    @Autowired private UserRepository userRepository;
//    @Autowired private ProductRepository productRepository;
//    @Autowired private RawMaterialRepository rawMaterialRepository;
//    @Autowired private ProductionWorkOrderRepository productionWorkOrderRepository;
//    @Autowired private EquipmentRepository equipmentRepository;
//    @Autowired private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
//    @Autowired private DispatchService dispatchService;
//    @Autowired private ModelMapper modelMapper;
//    private static final Logger logger = LoggerFactory.getLogger(QcOrderServiceImpl.class);
//
//
//    @Transactional
//    public QcOrderDTO createQcOrder(QcOrderDTO request) {
//        QcOrder qcOrder = modelMapper.map(request, QcOrder.class);
//        Integer userId = request.getCreatedBy();
//
//        // Create Dispatches & Map Associations
//        List<Dispatch> dispatches = request.getDispatches().stream()
//                .map(dispatchRequest -> dispatchService.createDispatch(dispatchRequest))
//                .collect(Collectors.toList());
//
//        // Map to QC Order Dispatch
//        qcOrder.setQcOrderDispatches(mapQcOrderDispatch(qcOrder, dispatches, userId, "CREATE"));
//
//        // Set work order state
//        qcOrder.setState(determineWorkOrderState(dispatches));
//
//        // Save QC Order
//        QcOrder savedQcOrder = qcOrderRepo.save(qcOrder);
//
//        return mapQcOrderToDTO(savedQcOrder);
//    }
//
//    @Transactional
//    public QcOrderDTO updateQcOrder(Long id, QcOrderDTO request) {
//        Optional<QcOrder> optionalQcOrder = qcOrderRepo.findById(id);
//        Integer userId = request.getUpdatedBy();
//
//        if (optionalQcOrder.isEmpty()) {
//            throw new RuntimeException("Order with ID " + id + " not found");
//        }
//
//        QcOrder existingQcOrder = optionalQcOrder.get();
//
//        if (request.getName() != null) {
//            existingQcOrder.setName(request.getName());
//        }
//        if (request.getDescription() != null) {
//            existingQcOrder.setDescription(request.getDescription());
//        }
//
//        if (request.getDispatches() != null) {
//            // Extract IDs from Request
//            List<Long> incomingDispatchIds = request.getDispatches().stream()
//                    .map(DispatchDTO::getId)
//                    .filter(Objects::nonNull)
//                    .toList();
//
//            // Soft Delete Missing Dispatches by soft delete junction row and soft delete dispatch since dispatch is designed to belong to qc order
//            List<QcOrderDispatch> toBeDeleted = existingQcOrder.getQcOrderDispatches().stream()
//                    .filter(qcOrderDispatch -> !incomingDispatchIds.contains(qcOrderDispatch.getDispatch().getId()))
//                    .peek(qcOrderDispatch -> {
//                        qcOrderDispatch.setUpdateDetails(userId, 0); // Mark as soft deleted
//                        dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId(), userId);
//                    })
//                    .toList();
//
//            // Persist soft deletes
//            qcOrderDispatchRepo.saveAll(toBeDeleted);
//
//            // Remove the soft-deleted ones from the list
//            existingQcOrder.getQcOrderDispatches().removeAll(toBeDeleted);
//
//            // Process Incoming Dispatches (Update or Create)
//            List<Dispatch> updatedOrCreatedDispatches = request.getDispatches().stream()
//                    .map(dispatchDTO -> (dispatchDTO.getId() != null) ?
//                            dispatchService.updateDispatch(dispatchDTO.getId(), dispatchDTO) :
//                            dispatchService.createDispatch(dispatchDTO))
//                    .toList();
//
//            // Merge New Dispatches with Retained Ones
//            updatedOrCreatedDispatches.forEach(dispatch -> {
//                boolean exists = existingQcOrder.getQcOrderDispatches().stream()
//                        .anyMatch(qcOrderDispatch -> qcOrderDispatch.getDispatch().getId().equals(dispatch.getId()));
//
//                if (!exists) {
//                    QcOrderDispatch newQcOrderDispatch = new QcOrderDispatch(existingQcOrder, dispatch);
//                    newQcOrderDispatch.setUpdateDetails(userId, 1);
//                    existingQcOrder.getQcOrderDispatches().add(newQcOrderDispatch);
//                }
//            });
//
//            existingQcOrder.setState(determineWorkOrderState(updatedOrCreatedDispatches));
//        }
//
//        return mapQcOrderToDTO(qcOrderRepo.save(existingQcOrder));
//    }
//
//    @Transactional(readOnly = true)
//    public QcOrderDTO getQcOrderById(Long id) {
//        Optional<QcOrder> qcOrder = qcOrderRepo.findByIdAndStatus(id, (short) 1);
//        if (qcOrder.isPresent()){
//            return mapQcOrderToDTO(qcOrder.get());
//        } else {
//            throw new EntityNotFoundException("QC Order with ID " + id + " not found");
//        }
//    }
//
//    @Transactional(readOnly = true)
//    public List<QcOrderDTO> getAllQcOrders() {
//        return qcOrderRepo.findAll()
//                .stream()
//                .filter(qcOrder -> qcOrder.getStatus() == 1)
//                .map(this::mapQcOrderToDTO).collect(Collectors.toList());
//    }
//
//    @Transactional
//    public void deleteQcOrder(Long orderId, Integer userId) {
//        QcOrder qcOrder = qcOrderRepo.findById(orderId)
//                .orElseThrow(() -> new EntityNotFoundException("QC Order not found"));
//
//        // Soft-delete order & associated dispatches
//        qcOrder.setStatus(0);
//        qcOrder.getQcOrderDispatches().forEach(qcOrderDispatch -> {
//            dispatchService.deleteDispatch(qcOrderDispatch.getDispatch().getId(), userId);
//            qcOrderDispatch.setStatus(0);
//        });
//
//        qcOrderRepo.save(qcOrder);
//    }
//
//    // Helper: Map QC Order to Dispatches
//    private List<QcOrderDispatch> mapQcOrderDispatch(QcOrder qcOrder, List<Dispatch> dispatches, Integer userId, String action) {
//        return dispatches.stream()
//                .map(dispatch -> {
//                    QcOrderDispatch qcOrderDispatch = new QcOrderDispatch(qcOrder, dispatch);
//                    if ("CREATE".equals(action)) {
//                        qcOrderDispatch.setCreationDetails(userId, 1);
//                    } else {
//                        qcOrderDispatch.setUpdateDetails(userId, 1);
//                    }
//                    return qcOrderDispatch;
//                })
//                .toList();
//    }
//
//    // Helper: Convert QcOrder to QcOrderDTO
//    private QcOrderDTO mapQcOrderToDTO(QcOrder qcOrder) {
//        QcOrderDTO qcOrderDTO = modelMapper.map(qcOrder, QcOrderDTO.class);
//
//        List<DispatchDTO> dispatchDTOs = qcOrder.getQcOrderDispatches().stream()
//                .map(qcOrderDispatch -> {
//                    if (qcOrderDispatch.getDispatch().getStatus() == 1) {
//                        return dispatchService.convertToDispatchDTO(qcOrderDispatch.getDispatch());
//                    } else {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .toList();
//
//        qcOrderDTO.setDispatches(dispatchDTOs);
//        return qcOrderDTO;
//    }
//
//    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
//    @Transactional
//    public void updateQcOrderStates() {
//        List<QcOrder> qcOrders = qcOrderRepo.findAll();
//        qcOrders.forEach(qcOrder -> {
//            List<Dispatch> dispatches = qcOrder.getQcOrderDispatches().stream()
//                    .map(QcOrderDispatch::getDispatch)
//                    .toList();
//
//            Short newState = determineWorkOrderState(dispatches);
//            if (!qcOrder.getState().equals(newState)) {
//                qcOrder.setState(newState);
//                qcOrderRepo.save(qcOrder);
//            }
//        });
//    }
//
//    private Short determineWorkOrderState(List<Dispatch> dispatches) {
//        if (dispatches == null || dispatches.isEmpty()) {
//            return QcOrderState.Inactive.getState();
//        }
//
//        boolean hasActive = dispatches.stream().anyMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Active);
//        boolean hasInvalid = dispatches.stream().anyMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Invalid);
//        boolean allInactive = dispatches.stream().allMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Inactive);
//        boolean allExpired = dispatches.stream().allMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Expired);
//        boolean allExhausted = dispatches.stream().allMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Exhausted);
//        boolean allPaused = dispatches.stream().allMatch(d -> DispatchState.fromValue(d.getState()) == DispatchState.Paused);
//
//        if (hasInvalid) return QcOrderState.Invalid.getState();
//        if (hasActive) return QcOrderState.Active.getState();
//        if (allPaused) return QcOrderState.Paused.getState();
//        if (allExhausted) return QcOrderState.Exhausted.getState();
//        if (allExpired) return QcOrderState.Expired.getState();
//        return QcOrderState.Inactive.getState();
//    }
//
//}
