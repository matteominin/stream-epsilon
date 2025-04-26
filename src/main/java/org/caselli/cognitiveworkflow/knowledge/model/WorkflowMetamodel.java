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

@Data
@Document(collection = "meta_workflows")
@CompoundIndex(name = "handledIntents_intentId_idx", def = "{'handledIntents.intentId': 1}")
public class WorkflowMetamodel {
    @Id
    @Field("_id")
    private String id;

    // DAG
    @Field("nodes") private List<WorkflowNode> nodes;
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

    @Data
    public static class WorkflowNode {
        private String nodeId;
        private Map<String, Object> configurationOverrides;
    }

    @Data
    public static class WorkflowIntentCapability {
        private String intentId;
        private Date lastExecuted;
        private Double score;
    }

}
