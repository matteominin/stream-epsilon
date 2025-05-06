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

}
