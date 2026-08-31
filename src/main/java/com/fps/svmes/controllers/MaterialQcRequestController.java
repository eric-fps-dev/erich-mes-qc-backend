package com.fps.svmes.controllers;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListItemDTO;
import com.fps.svmes.exceptions.MaterialQcRequestNotFoundException;
import com.fps.svmes.models.sql.production.MaterialQcRequest;
import com.fps.svmes.services.MaterialQcRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Material QC request lifecycle: draft -> pending (confirm) -> passed | failed (result).
 */
@RestController
@RequestMapping("/material-qc-requests")
@RequiredArgsConstructor
public class MaterialQcRequestController {

    private final MaterialQcRequestService service;

    @GetMapping
    public PagedResultDTO<MaterialQcRequest> findAll(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String materialLotNumber,
            @RequestParam(required = false) Long formTemplateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.findAll(materialId, materialLotNumber, formTemplateId, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MaterialQcRequest request = service.findById(id);
        return request != null ? ResponseEntity.ok(request) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MaterialQcRequest request) {
        try {
            return ResponseEntity.ok(service.create(request));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Invalid material QC request — check required fields (materialId, materialLotNumber)");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MaterialQcRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (MaterialQcRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Invalid material QC request — check required fields (materialId, materialLotNumber)");
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This request was modified by another request — reload and try again");
        }
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.confirm(id));
        } catch (MaterialQcRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This request was modified by another request — reload and try again");
        }
    }

    @PostMapping("/{id}/result")
    public ResponseEntity<?> setResult(@PathVariable Long id, @RequestBody(required = false) Map<String, Boolean> body) {
        try {
            Boolean passed = body != null ? body.get("passed") : null;
            if (passed == null) {
                return ResponseEntity.badRequest().body("\"passed\" is required");
            }
            return ResponseEntity.ok(service.setResult(id, passed));
        } catch (MaterialQcRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This request was modified by another request — reload and try again");
        }
    }

    @GetMapping("/{id}/related-records")
    public ResponseEntity<?> getRelatedRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PagedResultDTO<ApprovalInstanceListItemDTO> result = service.getRelatedRecords(id, page, size);
            return ResponseEntity.ok(result);
        } catch (MaterialQcRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
