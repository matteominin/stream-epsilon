package org.caselli.cognitiveworkflow.knowledge.model.workflow;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

/**
 * Represents a workflow metamodel, containing a DAG of nodes and edges,
 * the list of intents it can handle, and metadata information.
 */
@Data
@Document(collection = "meta_workflows")
@CompoundIndex(name = "handledIntents_intentId_idx", def = "{'handledIntents.intentId': 1}")
public class WorkflowMetamodel {
    @Id private String id;

    /**
     * List of nodes included in this workflow.
     * Each node represents a dependency that defines a reference to a meta-node and
     * its specific configuration for this workflow.
     */
    @NotNull
    @Field("nodes")
    private List<WorkflowNode> nodes;

    /**
     * List of edges representing the connections (transitions) between nodes in the DAG workflow.
     */
    @NotNull
    @Field("edges")
    private List<WorkflowEdge> edges;

    // Intents
    private List<WorkflowIntentCapability> handledIntents;

    // Meta data
    @NotNull private String name;
    @NotNull private String description;
    @NotNull private Boolean enabled;
    @NotNull private Version version;

    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    /**
     * Represents a handled intent capability of the workflow for a specific intent.
     */
    @Data
    public static class WorkflowIntentCapability {
        private String intentId;
        private Date lastExecuted;
        private Double score;
    }


    /**
     * Get the entry nodes of the workflow
     * (no incoming edges)
     * @return Returns the IDs of the entry WorkflowNodes
     */
    public Set<String> getEntryNodes() {
        if (this.getNodes() == null || this.getEdges() == null) return Collections.emptySet();

        Set<String> allNodeIds = this.getNodes().stream()
                .map(WorkflowNode::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> nodesWithIncomingEdges = this.getEdges().stream()
                .map(WorkflowEdge::getTargetNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());


        return allNodeIds.stream()
                .filter(id1 -> !nodesWithIncomingEdges.contains(id1))
                .collect(Collectors.toSet());
    }

    /**
     * Get the exit nodes of the workflow
     * (no outgoing edges)
     * @return Returns the IDs of the exit WorkflowNodes
     */
    public Set<String> getExitNodes() {
        if (this.getNodes() == null || this.getEdges() == null) return Collections.emptySet();

        Set<String> allNodeIds = this.getNodes().stream()
                .map(WorkflowNode::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> nodesWithOutgoingEdges = this.getEdges().stream()
                .map(WorkflowEdge::getSourceNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allNodeIds.stream()
                .filter(id1 -> !nodesWithOutgoingEdges.contains(id1))
                .collect(Collectors.toSet());
    }

    /**
     * Detects if the nodes of a workflow have changed between two metamodel versions.
     * This method compares the structure and content of nodes to determine if there are breaking changes.
     * @param oldMetamodel The previous version of the workflow metamodel
     * @param newMetamodel The updated version of the workflow metamodel
     * @return true if nodes have changed in a way that affects workflow structure, false otherwise
     */
    public static boolean haveNodesChanged(WorkflowMetamodel oldMetamodel, WorkflowMetamodel newMetamodel) {
        // Null safety checks
        if (oldMetamodel == null || newMetamodel == null) {
            return true;
        }

        List<WorkflowNode> oldNodes = oldMetamodel.getNodes();
        List<WorkflowNode> newNodes = newMetamodel.getNodes();

        // Handle null node lists
        if (oldNodes == null && newNodes == null) return false;
        if (oldNodes == null || newNodes == null) return true;

        // Check if the number of nodes changed
        if (oldNodes.size() != newNodes.size()) return true;


        Map<String, WorkflowNode> oldNodeMap = oldNodes.stream()
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(WorkflowNode::getId, node -> node));

        Map<String, WorkflowNode> newNodeMap = newNodes.stream()
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(WorkflowNode::getId, node -> node));


        if (!oldNodeMap.keySet().equals(newNodeMap.keySet())) return true;

        // Check each node for changes in content
        for (String nodeId : oldNodeMap.keySet()) {
            WorkflowNode oldNode = oldNodeMap.get(nodeId);
            WorkflowNode newNode = newNodeMap.get(nodeId);

            if (hasNodeChanged(oldNode, newNode)) return true;
        }

        return false;
    }

    /**
     * Compares two individual WorkflowNode instances to detect changes.
     * @param oldNode The old version of the node
     * @param newNode The new version of the node
     * @return true if the node has changed, false otherwise
     */
    private static boolean hasNodeChanged(WorkflowNode oldNode, WorkflowNode newNode) {
        if (oldNode == null && newNode == null) return false;
        if (oldNode == null || newNode == null) return true;

        // Compare node metamodel ID
        return !Objects.equals(oldNode.getNodeMetamodelId(), newNode.getNodeMetamodelId());
    }
}
