//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.dispatch.InstrumentDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.InstrumentService;
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
// * REST Controller for managing instruments.
// */
//@RestController
//@RequestMapping("/instrument")
//@Tag(name = "Instrument API", description = "API for managing quality control instruments")
//public class InstrumentController {
//
//    @Autowired
//    private InstrumentService instrumentService;
//
//    private static final Logger logger = LoggerFactory.getLogger(InstrumentController.class);
//
//    @Operation(summary = "Create a new instrument", description = "Creates a new instrument in the QC system")
//    @PostMapping
//    public ResponseResult<InstrumentDTO> createInstrument(@RequestBody @Valid InstrumentDTO instrumentDTO) {
//        try {
//            InstrumentDTO instrument = instrumentService.createInstrument(instrumentDTO);
//            return ResponseResult.success(instrument);
//        } catch (Exception e) {
//            logger.error("Error creating instrument", e);
//            return ResponseResult.fail("Failed to create instrument", e);
//        }
//    }
//
//    @Operation(summary = "Get a single instrument by ID", description = "Retrieves an active instrument by its ID")
//    @GetMapping("/{id}")
//    public ResponseResult<InstrumentDTO> getInstrumentById(@PathVariable Long id) {
//        try {
//            return ResponseResult.success(instrumentService.getInstrumentById(id));
//            }
//        catch (Exception e) {
//            logger.error("Error retrieving instrument with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve instrument", e);
//        }
//    }
//
//    @Operation(summary = "Get all active instruments", description = "Retrieves a list of all active instruments")
//    @GetMapping
//    public ResponseResult<List<InstrumentDTO>> getAllInstruments() {
//        try {
//            List<InstrumentDTO> instrumentDTOS = instrumentService.getAllInstruments();
//            return instrumentDTOS.isEmpty()
//                    ? ResponseResult.noContent(instrumentDTOS)
//                    : ResponseResult.success(instrumentDTOS);
//        } catch (Exception e) {
//            logger.error("Error retrieving all instruments", e);
//            return ResponseResult.fail("Failed to retrieve all instruments", e);
//        }
//    }
//
//    @Operation(summary = "Update an instrument", description = "Updates an existing instrument by ID")
//    @PutMapping("/{id}")
//    public ResponseResult<InstrumentDTO> updateInstrument(@PathVariable Long id, @RequestBody @Valid InstrumentDTO instrumentDTO) {
//        try {
//            InstrumentDTO updatedInstrument = instrumentService.updateInstrument(id, instrumentDTO);
//            return ResponseResult.success(updatedInstrument);
//        } catch (EntityNotFoundException e) {
//            logger.error("Instrument not found with ID: {}", id, e);
//            return ResponseResult.fail("Instrument not found with ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error updating instrument with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update instrument", e);
//        }
//    }
//
//    @Operation(summary = "Soft delete an instrument", description = "Marks an instrument as inactive instead of deleting it permanently")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteInstrument(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            instrumentService.deleteInstrument(id, userId);
//            return ResponseResult.success("Instrument with ID " + id + " has been deactivated.");
//        } catch (EntityNotFoundException e) {
//            logger.error("Instrument not found with ID: {}", id, e);
//            return ResponseResult.fail("Instrument not found with ID: " + id, e);
//        } catch (Exception e) {
//            logger.error("Error deleting instrument with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete instrument", e);
//        }
//    }
//}
