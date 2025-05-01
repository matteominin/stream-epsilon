package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowNode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * Represents a workflow metamodel, containing a DAG of nodes and edges,
 * the list of intents it can handle, and metadata information.
 */
@Data
@Document(collection = "meta_workflows")
@CompoundIndex(name = "handledIntents_intentId_idx", def = "{'handledIntents.intentId': 1}")
public class WorkflowMetamodel {
    @Field("_id")
    @Id
    private String id = UUID.randomUUID().toString();

    /**
     * List of nodes included in this workflow.
     * Each node represents a dependency that defines a reference to a meta-node and
     * its specific configuration for this workflow.
     */
    @Field("nodes")
    private List<WorkflowNode> nodes;

    /**
     * List of edges representing the connections (transitions) between nodes in the DAG workflow.
     */
    @Field("edges") private List<WorkflowEdge> edges;

    // Intents
    private List<WorkflowIntentCapability> handledIntents;

    // Meta data
    private String name;
    private String description;
    private Boolean enabled;
    private Version version;
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
}
