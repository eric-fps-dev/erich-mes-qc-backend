package com.fps.svmes.controllers;

import com.fps.svmes.models.nosql.FormNode;
import com.fps.svmes.services.FormNodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/form-nodes")
@Tag(name = "Form Management API", description = "API for managing form nodes")
public class FormNodeController {

    @Autowired
    private FormNodeService service;

    // GET /form-nodes: Get all top-level nodes
    @GetMapping
    public List<FormNode> getAllNodes() {
        return service.getAllNodes();
    }

    // POST /form-nodes/top-level: Add a new top-level node (new document)
    @PostMapping("/top-level")
    public FormNode createTopLevelNode(@RequestBody FormNode node) {
        return service.saveNode(node);
    }

    // POST /form-nodes/child: Add a child node to an existing node
    @PostMapping("/child")
    public ResponseEntity<FormNode> addChildNode(@RequestParam String parentId, @RequestBody FormNode childNode) {
        Optional<FormNode> createdChild = service.addChildNode(parentId, childNode);
        return createdChild.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /form-nodes/{id}: Get a node by ID (traversing if necessary)
    @GetMapping("/{id}")
    public ResponseEntity<FormNode> getNodeById(@PathVariable String id) {
        Optional<FormNode> node = service.getNodeByIdOrUuid(id);
        return node.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /form-nodes/{id}: Delete a node by ID (traversing if necessary)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable String id) {
        boolean deleted = service.deleteNodeByIdOrUuid(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // PATCH /form-nodes/{id}: Update a node by ID
    @PatchMapping("/{id}")
    public ResponseEntity<FormNode> updateNode(@PathVariable String id, @RequestBody FormNode updatedNode) {
        Optional<FormNode> updated = service.updateNodeById(id, updatedNode);
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<?> moveNode(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String newParentId = payload.get("newParentId");
        log.info("🔁 Move request: id={}, newParentId={}", id, newParentId);

        try {
            boolean moved = service.moveNode(id, newParentId);
            return moved ? ResponseEntity.ok().build() : ResponseEntity.badRequest().body("Move failed");
        } catch (Exception e) {
            log.error("❌ Move failed for node: {}", id, e);
            return ResponseEntity.status(500).body("Server error");
        }
    }
}
