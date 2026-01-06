//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.dispatch.QcOrderDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.DispatchService;
//import com.fps.svmes.services.QcOrderService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.validation.Valid;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * REST Controller for managing QC Orders.
// */
//@RestController
//@RequestMapping("/qc-order")
//@Tag(name = "QC Order API", description = "API for QC Orders and related Dispatch operations")
//public class QcOrderController {
//
//    @Autowired
//    private QcOrderService qcOrderService;
//
//
//    @Autowired
//    DispatchService dispatchService;
//    private static final Logger logger = LoggerFactory.getLogger(QcOrderController.class);
//
//    /**
//     * Creates a new QC Order with its Dispatches.
//     */
//    @Operation(summary = "Create a new QC Order", description = "Creates a QC Order along with its dispatches.")
//    @PostMapping
//    public ResponseResult<QcOrderDTO> createQcOrder(@RequestBody @Valid QcOrderDTO request) {
//        try {
//            QcOrderDTO qcOrderDTO = qcOrderService.createQcOrder(request);
//            return ResponseResult.success(qcOrderDTO);
//        } catch (Exception e) {
//            logger.error("Error creating QC Order", e);
//            return ResponseResult.fail("Failed to create QC Order", e);
//        }
//    }
//
//    /**
//     * Updates an existing QC Order and its Dispatches.
//     */
//    @Operation(summary = "Update an existing QC Order", description = "Updates a QC Order and its associated dispatches.")
//    @PutMapping("/{id}")
//    public ResponseResult<QcOrderDTO> updateQcOrder(@PathVariable Long id, @RequestBody @Valid QcOrderDTO request) {
//        try {
//            QcOrderDTO updatedQcOrder = qcOrderService.updateQcOrder(id, request);
//            return ResponseResult.success(updatedQcOrder);
//        } catch (EntityNotFoundException e) {
//            logger.error("QC Order not found with ID: {}", id, e);
//            return ResponseResult.fail("QC Order not found", e);
//        } catch (Exception e) {
//            logger.error("Error updating QC Order with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update QC Order", e);
//        }
//    }
//
//    /**
//     * Retrieves a single QC Order by ID.
//     */
//    @Operation(summary = "Get QC Order by ID", description = "Fetches a QC Order along with its associated dispatches.")
//    @GetMapping("/{id}")
//    public ResponseResult<QcOrderDTO> getQcOrderById(@PathVariable Long id) {
//        try {
//            QcOrderDTO qcOrderDTO = qcOrderService.getQcOrderById(id);
//            return ResponseResult.success(qcOrderDTO);
//        } catch (EntityNotFoundException e) {
//            logger.error("QC Order not found with ID: {}", id, e);
//            return ResponseResult.fail("QC Order not found", e);
//        } catch (Exception e) {
//            logger.error("Error retrieving QC Order with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve QC Order", e);
//        }
//    }
//
//    /**
//     * Retrieves a list of all QC Orders.
//     */
//    @Operation(summary = "Get all QC Orders", description = "Retrieves all QC Orders with their associated dispatches.")
//    @GetMapping
//    public ResponseResult<List<QcOrderDTO>> getAllQcOrders() {
//        try {
//            List<QcOrderDTO> qcOrders = qcOrderService.getAllQcOrders();
//            return qcOrders.isEmpty()
//                    ? ResponseResult.noContent(qcOrders)
//                    : ResponseResult.success(qcOrders);
//        } catch (Exception e) {
//            logger.error("Error retrieving all QC Orders", e);
//            return ResponseResult.fail("Failed to retrieve QC Orders", e);
//        }
//    }
//
//    /**
//     * Deletes a QC Order by ID.
//     */
//    @Operation(summary = "Delete a QC Order", description = "Soft deletes a QC Order and its associated dispatches.")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteQcOrder(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            qcOrderService.deleteQcOrder(id, userId);
//            return ResponseResult.success("QC Order deleted successfully.");
//        } catch (EntityNotFoundException e) {
//            logger.error("QC Order not found with ID: {}", id, e);
//            return ResponseResult.fail("QC Order not found", e);
//        } catch (Exception e) {
//            logger.error("Error deleting QC Order with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete QC Order", e);
//        }
//    }
//
//    @PostMapping("/update-qc-order-states")
//    public ResponseEntity<String> updateQcOrderStates() {
//        qcOrderService.updateQcOrderStates();
//        return ResponseEntity.ok("QC Order states updated successfully.");
//    }
//
//
//}
