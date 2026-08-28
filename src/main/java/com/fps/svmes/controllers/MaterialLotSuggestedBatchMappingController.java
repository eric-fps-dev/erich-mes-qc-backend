package com.fps.svmes.controllers;

import com.fps.svmes.models.sql.production.MaterialLotSuggestedBatchMapping;
import com.fps.svmes.services.MaterialLotSuggestedBatchMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for the loose mapping between an inventory-service material lot (identified by
 * its lot number string, no FK) and a local SuggestedBatch.
 */
@RestController
@RequestMapping("/material-lot-suggested-batch-mappings")
@RequiredArgsConstructor
public class MaterialLotSuggestedBatchMappingController {

    private final MaterialLotSuggestedBatchMappingService service;

    @GetMapping
    public List<MaterialLotSuggestedBatchMapping> findAll(@RequestParam(required = false) String materialLotNumber) {
        if (materialLotNumber != null && !materialLotNumber.isBlank()) {
            MaterialLotSuggestedBatchMapping mapping = service.findByMaterialLotNumber(materialLotNumber);
            return mapping == null ? List.of() : List.of(mapping);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MaterialLotSuggestedBatchMapping mapping = service.findById(id);
        return mapping != null ? ResponseEntity.ok(mapping) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MaterialLotSuggestedBatchMapping mapping) {
        try {
            return ResponseEntity.ok(service.create(mapping));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material lot is already mapped to a suggested batch");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MaterialLotSuggestedBatchMapping mapping) {
        try {
            return ResponseEntity.ok(service.update(id, mapping));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material lot is already mapped to a suggested batch");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
