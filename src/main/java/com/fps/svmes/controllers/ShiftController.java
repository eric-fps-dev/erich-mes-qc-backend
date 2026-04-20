//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.user.ShiftDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.ShiftService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.persistence.EntityNotFoundException;
//import jakarta.validation.Valid;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * REST Controller for managing work shifts.
// */
//@RestController
//@RequestMapping("/shift")
//@Tag(name = "Shift API", description = "API for managing shifts in the QC system")
//public class ShiftController {
//
//    @Autowired
//    private ShiftService shiftService;
//
//    private static final Logger logger = LoggerFactory.getLogger(ShiftController.class);
//
//    @Operation(summary = "Create a new shift", description = "Creates a new shift in the QC system")
//    @PostMapping
//    public ResponseResult<ShiftDTO> createShift(@RequestBody @Valid ShiftDTO shiftDTO) {
//        try {
//            ShiftDTO createdShift = shiftService.createShift(shiftDTO);
//            return ResponseResult.success(createdShift);
//        } catch (Exception e) {
//            logger.error("Error creating shift", e);
//            return ResponseResult.fail("Failed to create shift", e);
//        }
//    }
//
//    @Operation(summary = "Get a single shift by ID", description = "Retrieves an active shift by its ID")
//    @GetMapping("/{id}")
//    public ResponseResult<ShiftDTO> getShiftById(@PathVariable Integer id) {
//        try {
//            return ResponseResult.success(shiftService.getShiftById(id));
//        } catch (Exception e) {
//            logger.error("Error retrieving shift with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve shift", e);
//        }
//    }
//
//    @Operation(summary = "Get all active shifts", description = "Retrieves a list of all active shifts")
//    @GetMapping
//    public ResponseResult<List<ShiftDTO>> getAllShifts() {
//        try {
//            List<ShiftDTO> shiftDTOS = shiftService.getAllShifts();
//            return shiftDTOS.isEmpty()
//                    ? ResponseResult.noContent(shiftDTOS)
//                    : ResponseResult.success(shiftDTOS);
//        } catch (Exception e) {
//            logger.error("Error retrieving all shifts", e);
//            return ResponseResult.fail("Failed to retrieve all shifts", e);
//        }
//    }
//
//    @Operation(summary = "Update a shift", description = "Updates an existing shift by ID")
//    @PutMapping("/{id}")
//    public ResponseResult<ShiftDTO> updateShift(@PathVariable Integer id, @RequestBody @Valid ShiftDTO shiftDTO) {
//        try {
//            ShiftDTO updatedShift = shiftService.updateShift(id, shiftDTO);
//            return ResponseResult.success(updatedShift);
//        } catch (EntityNotFoundException e) {
//            logger.error("Shift not found with ID: {}", id, e);
//            return ResponseResult.fail("Shift not found with ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error updating shift with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update shift", e);
//        }
//    }
//
//    @Operation(summary = "Soft delete a shift", description = "Marks a shift as inactive instead of deleting it permanently")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteShift(@PathVariable Integer id, @PathVariable Integer userId) {
//        try {
//            shiftService.deleteShift(id, userId);
//            return ResponseResult.success("Shift with ID " + id + " has been deactivated.");
//        } catch (EntityNotFoundException e) {
//            logger.error("Shift not found with ID: {}", id, e);
//            return ResponseResult.fail("Shift not found with ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error deleting shift with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete shift", e);
//        }
//    }
//}
//
