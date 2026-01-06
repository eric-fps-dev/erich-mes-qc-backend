//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.dispatch.TestSubjectDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.services.TestSubjectService;
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
//@RequestMapping("/test-subjects")
//@Tag(name = "Test Subject API", description = "API for managing test subjects")
//public class TestSubjectController {
//
//    @Autowired
//    private TestSubjectService service;
//
//    private static final Logger logger = LoggerFactory.getLogger(TestSubjectController.class);
//
//    @Operation(summary = "Create a new test subject", description = "Adds a new test subject")
//    @PostMapping
//    public ResponseResult<TestSubjectDTO> createTestSubject(@RequestBody TestSubjectDTO testSubjectDTO) {
//        try {
//            TestSubjectDTO createdSubject = service.createTestSubject(testSubjectDTO);
//            return ResponseResult.success(createdSubject);
//        } catch (Exception e) {
//            logger.error("Error creating test subject", e);
//            return ResponseResult.fail("Failed to create test subject", e);
//        }
//    }
//
//    @Operation(summary = "Get a test subject by ID", description = "Retrieves an active test subject by its ID")
//    @GetMapping("/{id}")
//    public ResponseResult<TestSubjectDTO> getTestSubjectById(@PathVariable Long id) {
//        try {
//            TestSubjectDTO testSubject = service.getTestSubjectById(id);
//            return ResponseResult.success(testSubject);
//        } catch (Exception e) {
//            logger.error("Error retrieving test subject with ID: {}", id, e);
//            return ResponseResult.fail("Failed to retrieve test subject", e);
//        }
//    }
//
//    @Operation(summary = "Get all active test subjects", description = "Retrieves a list of all active test subjects")
//    @GetMapping
//    public ResponseResult<List<TestSubjectDTO>> getAllActiveTestSubjects() {
//        try {
//            List<TestSubjectDTO> testSubjects = service.getAllActiveTestSubjects();
//            return ResponseResult.success(testSubjects);
//        } catch (Exception e) {
//            logger.error("Error retrieving all test subjects", e);
//            return ResponseResult.fail("Failed to retrieve all test subjects", e);
//        }
//    }
//
//    @Operation(summary = "Update a test subject", description = "Updates an existing test subject by ID")
//    @PutMapping("/{id}")
//    public ResponseResult<TestSubjectDTO> updateTestSubject(@PathVariable Long id, @RequestBody TestSubjectDTO testSubjectDTO) {
//        try {
//            TestSubjectDTO updatedTestSubject = service.updateTestSubject(id, testSubjectDTO);
//            return ResponseResult.success(updatedTestSubject);
//        } catch (Exception e) {
//            logger.error("Error updating test subject with ID: {}", id, e);
//            return ResponseResult.fail("Failed to update test subject", e);
//        }
//    }
//
//    @Operation(summary = "Soft delete a test subject", description = "Marks a test subject as inactive instead of permanently deleting it")
//    @DeleteMapping("/{id}/{userId}")
//    public ResponseResult<String> deleteTestSubject(@PathVariable Long id, @PathVariable Integer userId) {
//        try {
//            service.deleteTestSubject(id, userId);
//            return ResponseResult.success("Test Subject with ID " + id + " has been deactivated.");
//        } catch (Exception e) {
//            logger.error("Error deleting test subject with ID: {}", id, e);
//            return ResponseResult.fail("Failed to delete test subject", e);
//        }
//    }
//}
//
