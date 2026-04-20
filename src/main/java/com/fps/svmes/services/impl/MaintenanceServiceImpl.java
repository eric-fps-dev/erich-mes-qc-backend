//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.maintenance.EquipmentDTO;
//import com.fps.svmes.dto.dtos.maintenance.MaintenanceWorkOrderDTO;
//import com.fps.svmes.dto.dtos.production.ProductionWorkOrderDTO;
//import com.fps.svmes.models.sql.maintenance.Equipment;
//import com.fps.svmes.models.sql.maintenance.MaintenanceWorkOrder;
//import com.fps.svmes.models.sql.production.ProductionWorkOrder;
//import com.fps.svmes.repositories.jpaRepo.maintenance.EquipmentRepository;
//import com.fps.svmes.repositories.jpaRepo.maintenance.MaintenanceWorkOrderRepository;
//import com.fps.svmes.services.MaintenanceService;
//import jakarta.persistence.EntityNotFoundException;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class MaintenanceServiceImpl implements MaintenanceService {
//
//    @Autowired
//    private MaintenanceWorkOrderRepository maintenanceWorkOrderRepository;
//
//    @Autowired
//    private EquipmentRepository equipmentRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    /**
//     * Fetch all active maintenance work orders (status = 1).
//     *
//     * @return List of MaintenanceWorkOrderDTO
//     */
//    @Transactional(readOnly = true)
//    public List<MaintenanceWorkOrderDTO> getAllMaintenanceWorkOrders() {
//        return maintenanceWorkOrderRepository.findByStatus(1)
//                .stream()
//                .map(workOrder -> modelMapper.map(workOrder, MaintenanceWorkOrderDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Fetch a specific maintenance work order by ID (status = 1).
//     *
//     * @param id Maintenance Work Order ID
//     * @return MaintenanceWorkOrderDTO
//     */
//    @Transactional(readOnly = true)
//    public MaintenanceWorkOrderDTO getMaintenanceWorkOrderById(Integer id) {
//        try {
//            MaintenanceWorkOrder workOrder = maintenanceWorkOrderRepository.findByIdAndStatus(id, 1);
//            return modelMapper.map(workOrder, MaintenanceWorkOrderDTO.class);
//        } catch (Exception e) {
//            throw new EntityNotFoundException("Maintenance work order with ID " + id + " not found");
//        }
//    }
//
//    /**
//     * Fetch all active equipment (status = 1).
//     *
//     * @return List of EquipmentDTO
//     */
//    @Transactional(readOnly = true)
//    public List<EquipmentDTO> getAllEquipments() {
//        return equipmentRepository.findByStatus(1)
//                .stream()
//                .map(equipment -> modelMapper.map(equipment, EquipmentDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Fetch a specific equipment by ID (status = 1).
//     *
//     * @param id Equipment ID
//     * @return EquipmentDTO
//     */
//    @Transactional(readOnly = true)
//    public EquipmentDTO getEquipmentById(Integer id) {
//        try {
//            Equipment equipment = equipmentRepository.findByIdAndStatus(id, 1);
//            return modelMapper.map(equipment, EquipmentDTO.class);
//        } catch (Exception e) {
//            throw new EntityNotFoundException("Equipment with ID " + id + " not found");
//        }
//    }
//}
