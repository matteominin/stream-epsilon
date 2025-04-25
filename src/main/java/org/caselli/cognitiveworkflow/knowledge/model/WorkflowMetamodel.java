package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "meta_workflows")
public class WorkflowMetamodel {
    @Id
    @Field("_id")
    private String id;

    // DAG
    @Field("nodes") private List<WorkflowNode> nodes;
    @Field("edges") private List<WorkflowEdge> edges;

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
}
