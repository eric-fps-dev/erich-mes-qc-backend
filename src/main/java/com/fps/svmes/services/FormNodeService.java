package com.fps.svmes.services;

import com.fps.svmes.models.nosql.FormNode;

import java.util.List;
import java.util.Optional;

public interface FormNodeService {
    List<FormNode> getAllNodes();
    FormNode saveNode(FormNode node);
    Optional<FormNode> addChildNode(String parentId, FormNode childNode);
    Optional<FormNode> getNodeByIdOrUuid(String id);
    boolean deleteNodeByIdOrUuid(String id);
    List<FormNode> getNodesWithLabelContaining(String keyword);
    Optional<FormNode> updateNodeById(String id, FormNode updatedNode);
    boolean moveNode(String nodeId, String newParentId);

    void updateLabelByQcFormTemplateId(Long qcFormTemplateId, String newLabel);

    Optional<String> findParentNodeId(String nodeId);
}
