//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.maintenance.EquipmentDTO;
//import com.fps.svmes.dto.dtos.maintenance.MaintenanceWorkOrderDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.MaintenanceService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/maintenance")
//@Tag(name = "Maintenance Module API", description = "API for Maintenance Module")
//public class MaintenanceController {
//
//    @Autowired
//    private MaintenanceService maintenanceService;
//
//    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);
//
//    /**
//     * Get all active maintenance work orders.
//     *
//     * @return List of MaintenanceWorkOrderDTO
//     */
//    @GetMapping("/workorders")
//    @Operation(summary = "Get all active maintenance work orders", description = "Fetch all maintenance work orders with status = 1")
//    public ResponseResult<List<MaintenanceWorkOrderDTO>> getAllMaintenanceWorkOrders() {
//        try {
//            List<MaintenanceWorkOrderDTO> workOrders = maintenanceService.getAllMaintenanceWorkOrders();
//            return ResponseResult.success(workOrders);
//        } catch (Exception e) {
//            logger.error("Error retrieving all maintenance work orders", e);
//            return ResponseResult.fail("Failed to retrieve all maintenance work orders", e);
//        }
//    }
//
//    /**
//     * Get a specific maintenance work order by ID.
//     *
//     * @param id Maintenance Work Order ID
//     * @return MaintenanceWorkOrderDTO
//     */
//    @GetMapping("/workorders/{id}")
//    @Operation(summary = "Get a specific maintenance work order by ID", description = "Fetch a maintenance work order with status = 1 by its ID")
//    public ResponseResult<MaintenanceWorkOrderDTO> getMaintenanceWorkOrderById(@PathVariable Integer id) {
//        try {
//            MaintenanceWorkOrderDTO workOrder = maintenanceService.getMaintenanceWorkOrderById(id);
//            return ResponseResult.success(workOrder);
//        } catch (Exception e) {
//            logger.error("Error retrieving maintenance work order", e);
//            return ResponseResult.fail("Failed to retrieve maintenance work order", e);
//        }
//    }
//
//    /**
//     * Get all active equipment.
//     *
//     * @return List of EquipmentDTO
//     */
//    @GetMapping("/equipments")
//    @Operation(summary = "Get all active equipment", description = "Fetch all equipment with status = 1")
//    public ResponseResult<List<EquipmentDTO>> getAllEquipments() {
//        try {
//            List<EquipmentDTO> equipments = maintenanceService.getAllEquipments();
//            return ResponseResult.success(equipments);
//        } catch (Exception e) {
//            logger.error("Error retrieving all equipment", e);
//            return ResponseResult.fail("Failed to retrieve all equipment", e);
//        }
//    }
//
//    /**
//     * Get a specific equipment by ID.
//     *
//     * @param id Equipment ID
//     * @return EquipmentDTO
//     */
//    @GetMapping("/equipments/{id}")
//    @Operation(summary = "Get a specific equipment by ID", description = "Fetch specific equipment with status = 1 by its ID")
//    public ResponseResult<EquipmentDTO> getEquipmentById(@PathVariable Integer id) {
//        try {
//            EquipmentDTO equipment = maintenanceService.getEquipmentById(id);
//            return ResponseResult.success(equipment);
//        } catch (Exception e) {
//            logger.error("Error retrieving equipment", e);
//            return ResponseResult.fail("Failed to retrieve equipment", e);
//        }
//    }
//}
