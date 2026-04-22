//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.dispatch.SamplingLocationDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.SamplingLocationService;
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
//@RequestMapping("/sampling-locations")
//@Tag(name = "Sampling Location API", description = "API for managing sampling locations")
//public class SamplingLocationController {
//
//    @Autowired
//    private SamplingLocationService service;
//
//    private static final Logger logger = LoggerFactory.getLogger(SamplingLocationController.class);
//
//    @Operation(summary = "Create a new sampling location", description = "Adds a new sampling location")
//    @PostMapping
//    public ResponseResult<SamplingLocationDTO> createSamplingLocation(@RequestBody SamplingLocationDTO samplingLocationDTO) {
//        try {
//            SamplingLocationDTO createdLocation = service.createSamplingLocation(samplingLocationDTO);
//            return ResponseResult.success(createdLocation);
//        } catch (Exception e) {
//            logger.error("Error creating sampling location", e);
//            return ResponseResult.fail("Failed to create sampling location", e);
//        }
//    }
//
//    @Operation(summary = "Get a sampling location by ID", description = "Retrieves an active sampling location by its ID")
//    @GetMapping("/{id}")
//    public ResponseResult<SamplingLocationDTO> getSamplingLocationById(@PathVariable Long id) {
//        try {
//            SamplingLocationDTO location = service.getSamplingLocationById(id);
//            return ResponseResult.success(location);
//        } catch (Exception e) {
//            logger.error("Error retrieving sampling location with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve sampling location", e);
//        }
//    }
//
//    @Operation(summary = "Get all active sampling locations", description = "Retrieves a list of all active sampling locations")
//    @GetMapping
//    public ResponseResult<List<SamplingLocationDTO>> getAllActiveSamplingLocations() {
//        try {
//            List<SamplingLocationDTO> locations = service.getAllActiveSamplingLocations();
//            return ResponseResult.success(locations);
//        } catch (Exception e) {
//            logger.error("Error retrieving all sampling locations", e);
//            return ResponseResult.fail("Failed to retrieve all sampling locations", e);
//        }
//    }
//
//    @Operation(summary = "Update a sampling location", description = "Updates an existing sampling location by ID")
//    @PutMapping("/{id}")
//    public ResponseResult<SamplingLocationDTO> updateSamplingLocation(@PathVariable Long id, @RequestBody SamplingLocationDTO samplingLocationDTO) {
//        try {
//            SamplingLocationDTO updatedLocation = service.updateSamplingLocation(id, samplingLocationDTO);
//            return ResponseResult.success(updatedLocation);
//        } catch (Exception e) {
//            logger.error("Error updating sampling location with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update sampling location", e);
//        }
//    }
//
//    @Operation(summary = "Soft delete a sampling location", description = "Marks a sampling location as inactive instead of permanently deleting it")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteSamplingLocation(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            service.deleteSamplingLocation(id, userId);
//            return ResponseResult.success("Sampling Location with ID " + id + " has been deactivated.");
//        } catch (Exception e) {
//            logger.error("Error deleting sampling location with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete sampling location", e);
//        }
//    }
//}
//
