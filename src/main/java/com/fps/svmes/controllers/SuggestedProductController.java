package com.fps.svmes.controllers;

import com.fps.svmes.models.sql.production.SuggestedProduct;
import com.fps.svmes.services.SuggestedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suggested-products")
@RequiredArgsConstructor
public class SuggestedProductController {

    private final SuggestedProductService service;

    /**
     * Get all active suggested products (status = 1).
     * Commonly used by the frontend to only show valid entries.
     */
    @GetMapping("/active")
    public List<SuggestedProduct> findAllActive() {
        return service.findByStatus(1);
    }

    /**
     * Get all suggested products (both active and archived).
     * Useful for admin-level views or full dataset analysis.
     */
    @GetMapping
    public List<SuggestedProduct> findAll() {
        return service.findAll();
    }

    /**
     * Get a specific suggested product by its database ID.
     */
    @GetMapping("/{id}")
    public SuggestedProduct findById(@PathVariable Long id) {
        return service.findById(id);
    }

    /**
     * Get a suggested product by its code.
     * Returns 404 if the product code is not found.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<?> findByCode(@PathVariable String code) {
        SuggestedProduct result = service.findByCode(code);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }

    /**
     * Create a new suggested product.
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody SuggestedProduct product) {
        try {
            return ResponseEntity.ok(service.create(product));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Duplicate code");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Duplicate code");
        }
    }

    /**
     * Update an existing suggested product.
     */
    @PutMapping
    public SuggestedProduct update(@RequestBody SuggestedProduct product) {
        return service.update(product);
    }

    /**
     * Soft delete a suggested product by setting its status = 0.
     * This operation does not remove the record from the database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.ok().build();
    }
}
