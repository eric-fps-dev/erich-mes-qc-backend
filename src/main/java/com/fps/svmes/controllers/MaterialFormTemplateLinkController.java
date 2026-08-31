package com.fps.svmes.controllers;

import com.fps.svmes.models.sql.production.MaterialFormTemplateLink;
import com.fps.svmes.services.MaterialFormTemplateLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Links a material to its default QC form template. JSON shape matches the
 * existing frontend prototype ({@code materialId, materialName, scope,
 * defaultQcTemplateId, defaultQcTemplateName, active}).
 */
@RestController
@RequestMapping("/material-form-template-links")
@RequiredArgsConstructor
public class MaterialFormTemplateLinkController {

    private final MaterialFormTemplateLinkService service;

    @GetMapping
    public List<MaterialFormTemplateLink> find(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) Long materialId) {
        return service.find(scope, materialId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MaterialFormTemplateLink link = service.findById(id);
        return link != null ? ResponseEntity.ok(link) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MaterialFormTemplateLink link) {
        try {
            return ResponseEntity.ok(service.create(link));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material already has a linked template for this scope");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MaterialFormTemplateLink link) {
        try {
            return ResponseEntity.ok(service.update(id, link));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("Material already has a linked template for this scope");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
