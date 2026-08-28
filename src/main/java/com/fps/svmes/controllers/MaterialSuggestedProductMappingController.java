package com.fps.svmes.controllers;

import com.fps.svmes.models.sql.production.MaterialSuggestedProductMapping;
import com.fps.svmes.services.MaterialSuggestedProductMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for the loose mapping between an inventory-service material (id/name only,
 * no FK) and a local SuggestedProduct.
 */
@RestController
@RequestMapping("/material-suggested-product-mappings")
@RequiredArgsConstructor
public class MaterialSuggestedProductMappingController {

    private final MaterialSuggestedProductMappingService service;

    @GetMapping
    public List<MaterialSuggestedProductMapping> findAll(@RequestParam(required = false) Long materialId) {
        if (materialId != null) {
            MaterialSuggestedProductMapping mapping = service.findByMaterialId(materialId);
            return mapping == null ? List.of() : List.of(mapping);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MaterialSuggestedProductMapping mapping = service.findById(id);
        return mapping != null ? ResponseEntity.ok(mapping) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MaterialSuggestedProductMapping mapping) {
        try {
            return ResponseEntity.ok(service.create(mapping));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material is already mapped to a suggested product");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MaterialSuggestedProductMapping mapping) {
        try {
            return ResponseEntity.ok(service.update(id, mapping));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material is already mapped to a suggested product");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
