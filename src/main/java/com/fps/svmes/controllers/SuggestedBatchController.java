package com.fps.svmes.controllers;

import com.fps.svmes.models.sql.production.SuggestedBatch;
import com.fps.svmes.services.SuggestedBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suggested-batches")
@RequiredArgsConstructor
public class SuggestedBatchController {

    private final SuggestedBatchService service;

    /**
     * Get all active suggested batches (status = 1).
     * Used by the frontend to show non-archived options.
     */
    @GetMapping("/active")
    public List<SuggestedBatch> findAllActive() {
        return service.findByStatus(1);
    }

    /**
     * Get all suggested batches (including archived ones).
     */
    @GetMapping
    public List<SuggestedBatch> findAll() {
        return service.findAll();
    }

    /**
     * Get a suggested batch by its ID.
     */
    @GetMapping("/{id}")
    public SuggestedBatch findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /**
     * Get a suggested batch by its code.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<?> findByCode(@PathVariable String code) {
        SuggestedBatch result = service.findByCode(code);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    /**
     * Create a new suggested batch.
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody SuggestedBatch batch) {
        try {
            return ResponseEntity.ok(service.create(batch));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate code");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Duplicate code");
        }
    }

    /**
     * Update an existing suggested batch.
     */
    @PutMapping
    public SuggestedBatch update(@RequestBody SuggestedBatch batch) {
        return service.update(batch);
    }

    /**
     * Soft delete a suggested batch by setting status = 0.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.ok().build();
    }
}
