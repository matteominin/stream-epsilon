package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Map;

@Data
public class WorkflowEdge {
    @Id
    @Field("_id")
    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private Map<String,String> bindings;
}
