package com.fps.svmes.services.impl;

import com.fps.svmes.models.nosql.FormNode;
//import com.fps.svmes.repositories.jpaRepo.user.TeamFormRepository;
import com.fps.svmes.repositories.mongoRepo.FormNodeRepository;
import com.fps.svmes.services.FormNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.*;

@Service
public class FormNodeServiceImpl implements FormNodeService {

    @Autowired
    private FormNodeRepository repository;

    // TODO: add after adding missing data model in commmon module
//    @Autowired
//    private TeamFormRepository teamFormRepository;

    public static final Logger logger = LoggerFactory.getLogger(FormNodeServiceImpl.class);

    // Save a top-level node (new document)
    @Override
    public FormNode saveNode(FormNode node) {
        return repository.save(node);
    }

    // Get all top-level nodes
    @Override
    public List<FormNode> getAllNodes() {
        return repository.findAll();
    }

    // Add a child node to an existing node
    @Override
    public Optional<FormNode> addChildNode(String parentId, FormNode childNode) {
        List<FormNode> nodes = repository.findAll();
        for (FormNode node : nodes) {
            Optional<FormNode> createdChild = addChildNodeRecursively(node, parentId, childNode);
            if (createdChild.isPresent()) {
                repository.save(node); // Save the updated top-level node
                return createdChild;   // Return the newly created child node
            }
        }
        return Optional.empty();
    }

    // Get a node by ID or UUID (traverse if necessary)
    @Override
    public Optional<FormNode> getNodeByIdOrUuid(String id) {
        List<FormNode> nodes = repository.findAll();
        for (FormNode node : nodes) {
            Optional<FormNode> result = findNodeByIdOrUuid(node, id);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    // Delete a node by ID or UUID (traverse if necessary)
    @Override
    @Transactional
    public boolean deleteNodeByIdOrUuid(String id) {
        // Fetch all top-level nodes
        List<FormNode> nodes = repository.findAll();

        // Iterate over root nodes
        for (int i = 0; i < nodes.size(); i++) {
            // Check if the root node's ID matches the given ID
            if (nodes.get(i).getId().equals(id)) {
                // Clean team-form association for deleted form node
                List<String> formNodeArr = new ArrayList<>();
                collectFormIdsRecursively(nodes.get(i), formNodeArr);

                // TODO: add after adding missing data model in commmon module
                // teamFormRepository.deleteAllByFormIds(formNodeArr);

                nodes.remove(i); // Remove the root node
                repository.deleteById(id); // Persist the deletion
                return true; // Successfully deleted
            }

            // Recursively check within the children
            if (deleteNodeByIdOrUuid(nodes.get(i), id)) {
                repository.save(nodes.get(i)); // Persist changes to the updated tree
                return true;
            }
        }
        return false; // Node not found
    }


    // Recursive method to find a node by ID or UUID
    private Optional<FormNode> findNodeByIdOrUuid(FormNode currentNode, String id) {
        if (currentNode.getId().equals(id)) {
            return Optional.of(currentNode);
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                Optional<FormNode> result = findNodeByIdOrUuid(child, id);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    // Recursive method to delete a node by ID or UUID
    private boolean deleteNodeByIdOrUuid(FormNode currentNode, String id) {
        if (currentNode.getChildren() != null) {
            for (int i = 0; i < currentNode.getChildren().size(); i++) {
                FormNode child = currentNode.getChildren().get(i);
                if (child.getId().equals(id)) {
                    // Clean team-form association for deleted form node
                    List<String> formNodeArr = new ArrayList<>();
                    collectFormIdsRecursively(child, formNodeArr);

                    // TODO: add after adding missing data model in commmon module
//                    teamFormRepository.deleteAllByFormIds(formNodeArr);

                    currentNode.getChildren().remove(i); // Remove the matching child node

                    return true; // Successfully deleted
                }

                // Recursively search deeper
                if (deleteNodeByIdOrUuid(child, id)) {
                    return true;
                }
            }
        }
        return false; // Node not found in this subtree
    }

    // Recursive method to add a child node to a node with the given parentId
    private Optional<FormNode> addChildNodeRecursively(FormNode currentNode, String parentId, FormNode childNode) {
        if (currentNode.getId().equals(parentId)) {
            if (currentNode.getChildren() == null) {
                currentNode.setChildren(new ArrayList<>());
            }
            // Assign a UUID to the child node if it doesn't already have an ID
            if (childNode.getId() == null) {
                childNode.setId(UUID.randomUUID().toString());
            }
            currentNode.getChildren().add(childNode);
            return Optional.of(childNode); // Return the newly added child node
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                Optional<FormNode> result = addChildNodeRecursively(child, parentId, childNode);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<FormNode> updateNodeById(String id, FormNode updatedNode) {
        List<FormNode> nodes = repository.findAll(); // Fetch all top-level nodes

        for (FormNode root : nodes) {
            Optional<FormNode> updated = findAndUpdateNodeRecursively(root, id, updatedNode);
            if (updated.isPresent()) {
                repository.save(root); // Save the updated top-level document
                return updated;       // Return the updated node
            }
        }
        return Optional.empty(); // Node not found
    }

    @Override
    public boolean moveNode(String movingNodeId, String newParentId) {
        List<FormNode> roots = repository.findAll();

        FormNode movingNode = null;
        FormNode fromRoot = null;
        boolean isRootNode = false;

        // Step 1: Locate the moving node and its origin root
        Iterator<FormNode> rootIter = roots.iterator();
        while (rootIter.hasNext()) {
            FormNode root = rootIter.next();

            if (root.getId().equals(movingNodeId)) {
                movingNode = root;
                fromRoot = root;
                isRootNode = true;
                rootIter.remove(); // prepare for reinsertion
                break;
            }

            movingNode = extractAndRemoveNode(root, movingNodeId);
            if (movingNode != null) {
                fromRoot = root;
                break;
            }
        }

        if (movingNode == null) {
            logger.warn("❌ Moving node not found: {}", movingNodeId);
            return false;
        }

        // Special case: Move to top-level (i.e., no parent)
        if ("root".equalsIgnoreCase(newParentId)) {
            repository.save(movingNode); // save as a new root document
            if (!isRootNode) {
                repository.save(fromRoot); // persist where it was removed from
            } else {
                repository.deleteById(movingNodeId); // cleanup duplicate
            }
            return true;
        }

        // Prevent loop: do not move under its own descendant
        if (isDescendantOf(movingNode, newParentId)) {
            logger.warn("⛔ Cannot move a node into its own descendant: {}", newParentId);
            return false;
        }

        // Step 2: Find the target folder
        for (FormNode root : roots) {
            FormNode targetFolder = findNodeByIdOrUuid(root, newParentId).orElse(null);
            if (targetFolder != null && "folder".equals(targetFolder.getNodeType())) {
                if (targetFolder.getChildren() == null) {
                    targetFolder.setChildren(new ArrayList<>());
                }

                targetFolder.getChildren().add(movingNode);

                if (!isRootNode) {
                    repository.save(fromRoot);
                } else {
                    repository.deleteById(movingNodeId);
                }

                repository.save(root);
                return true;
            }
        }

        logger.warn("❌ Target folder not found or not a folder: {}", newParentId);
        return false;
    }


    private FormNode extractAndRemoveNode(FormNode current, String targetId) {
        if (current.getChildren() == null) return null;

        Iterator<FormNode> iterator = current.getChildren().iterator();
        while (iterator.hasNext()) {
            FormNode child = iterator.next();
            if (child.getId().equals(targetId)) {
                iterator.remove();
                return child;
            }
            FormNode result = extractAndRemoveNode(child, targetId);
            if (result != null) return result;
        }
        return null;
    }

    private boolean isDescendantOf(FormNode node, String possibleChildId) {
        if (node.getChildren() == null) return false;

        for (FormNode child : node.getChildren()) {
            if (child.getId().equals(possibleChildId)) return true;
            if (isDescendantOf(child, possibleChildId)) return true;
        }
        return false;
    }

    // Helper method for recursive traversal
    private Optional<FormNode> findAndUpdateNodeRecursively(FormNode currentNode, String id, FormNode updatedNode) {
        if (currentNode.getId().equals(id)) {
            // Update node properties
            if (updatedNode.getLabel() != null) currentNode.setLabel(updatedNode.getLabel());
            if (updatedNode.getNodeType() != null) currentNode.setNodeType(updatedNode.getNodeType());
            if (updatedNode.getChildren() != null) currentNode.setChildren(updatedNode.getChildren());
            if (updatedNode.getQcFormTemplateId() != null) currentNode.setQcFormTemplateId(updatedNode.getQcFormTemplateId());
            return Optional.of(currentNode);
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                Optional<FormNode> result = findAndUpdateNodeRecursively(child, id, updatedNode);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty(); // Node not found in this subtree
    }

    @Override
    public List<FormNode> getNodesWithLabelContaining(String keyword) {
        List<FormNode> allNodes = repository.findAll();
        List<FormNode> matchingNodes = new ArrayList<>();

        for (FormNode node : allNodes) {
            findMatchingNodesRecursively(node, keyword.toLowerCase(), matchingNodes);
        }

        return matchingNodes;
    }

    // Recursive helper method to search within child nodes
    private void findMatchingNodesRecursively(FormNode currentNode, String keyword, List<FormNode> matchingNodes) {
        if (currentNode.getLabel().toLowerCase().contains(keyword)) {
            matchingNodes.add(currentNode);
        }
        if (currentNode.getChildren() != null) {
            for (FormNode child : currentNode.getChildren()) {
                findMatchingNodesRecursively(child, keyword, matchingNodes);
            }
        }
    }

    // Grab all document type node ids for a target node.
    private void collectFormIdsRecursively(FormNode node, List<String> result) {
        if ("document".equalsIgnoreCase(node.getNodeType())) {
            result.add(node.getId());
        }

        if (node.getChildren() != null) {
            for (FormNode child : node.getChildren()) {
                collectFormIdsRecursively(child, result);
            }
        }
    }
}
