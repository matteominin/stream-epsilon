package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowEdge;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Represents a workflow metamodel, containing a DAG of nodes and edges,
 * the list of intents it can handle, and metadata information.
 */
@Data
@Document(collection = "meta_workflows")
@CompoundIndex(name = "handledIntents_intentId_idx", def = "{'handledIntents.intentId': 1}")
public class WorkflowMetamodel {
    @Id
    @Field("_id")
    private String id;


    /**
     * List of node dependencies included in this workflow.
     * Each dependency defines a reference to a node and its specific configuration for this workflow.
     */
    @Field("nodes")
    private List<WorkflowNodeDependency> nodes;

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
    private Date createdAt;
    private Date updatedAt;
    private Version version;


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
     * Represents a dependency of a workflow on a specific node.
     */
    @Data
    public static class WorkflowNodeDependency {
        private String nodeId;
        private Map<String, Object> configurationOverrides;
        private Version version;
    }
}
