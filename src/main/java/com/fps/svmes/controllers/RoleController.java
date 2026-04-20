//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.user.RoleDTO;
//import com.fps.svmes.dto.responses.ResponseResult;
//import com.fps.svmes.dto.responses.ResponseStatus;
//import com.fps.svmes.services.RoleService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@Slf4j
//@RequestMapping("/role")
//@RequiredArgsConstructor
//@Tag(name = "Role API", description = "API for Role Management")
//public class RoleController {
//
//    private final RoleService roleService;
//    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);
//
//    // GET - Retrieve all roles
//    @GetMapping("")
//    @Operation(summary = "Get all roles", description = "Returns all roles in the QC system")
//    public ResponseResult<List<RoleDTO>> getAllRoles() {
//        try {
//            List<RoleDTO> roleDTOList = roleService.getAllRoles();
//            logger.info("Roles retrieved, count: {}", roleDTOList.size());
//            return ResponseResult.success(roleDTOList);
//        } catch (Exception e) {
//            logger.error("Error retrieving roles", e);
//            return ResponseResult.fail("Error retrieving roles", e);
//        }
//    }
//
//    // POST - Create a new role
//    @PostMapping("")
//    @Operation(summary = "Create a new role", description = "Creates a new role in the QC system")
//    public ResponseResult<RoleDTO> createRole(@RequestBody RoleDTO roleDTO) {
//        try {
//            RoleDTO createdRole = roleService.createRole(roleDTO);
//            logger.info("Role created: {}", createdRole);
//            return ResponseResult.success(createdRole);
//        } catch (Exception e) {
//            logger.error("Error creating role", e);
//            return ResponseResult.fail("Error creating role", e);
//        }
//    }
//
//    // PUT - Update an existing role
//    @PutMapping("/{id}")
//    @Operation(summary = "Update a role", description = "Updates a role's information in the QC system")
//    public ResponseResult<RoleDTO> updateRole(@PathVariable Short id, @RequestBody RoleDTO roleDTO) {
//        try {
//            RoleDTO updatedRole = roleService.updateRole(id, roleDTO);
//            logger.info("Role updated: {}", updatedRole);
//            return ResponseResult.success(updatedRole);
//        } catch (Exception e) {
//            logger.error("Error updating role with ID: {}", id, e);
//            return ResponseResult.fail("Error updating role", e);
//        }
//    }
//
//    // DELETE - Delete a role by ID
//    @DeleteMapping("/{id}")
//    @Operation(summary = "Delete a role", description = "Deletes a role from the QC system")
//    public ResponseResult<Void> deleteRole(@PathVariable Short id) {
//        try {
//            roleService.deleteRole(id);
//            logger.info("Role deleted with ID: {}", id);
//            return ResponseResult.success(null);
//        } catch (Exception e) {
//            logger.error("Error deleting role with ID: {}", id, e);
//            return ResponseResult.fail("Error deleting role", e);
//        }
//    }
//
//    // GET - Retrieve role by ID
//    @GetMapping("/{id}")
//    @Operation(summary = "Get Role By ID", description = "Fetches a role's information by ID")
//    public ResponseResult<RoleDTO> getRoleById(@PathVariable Short id) {
//        try {
//            RoleDTO roleDTO = roleService.getRoleById(id);
//
//            if (roleDTO != null) {
//                logger.info("Role information retrieved for ID: {}", id);
//                return ResponseResult.success(roleDTO);
//            } else {
//                logger.warn("No role found with ID: {}", id);
//                return ResponseResult.fail("Role not found");
//            }
//        } catch (Exception e) {
//            logger.error("Error retrieving role with ID: {}", id, e);
//            return ResponseResult.fail("Error retrieving role information", e);
//        }
//    }
//}
