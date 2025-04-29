package org.caselli.cognitiveworkflow.knowledge.model.shared;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Represents a directed edge in the workflow graph
 */
@Data
public class WorkflowEdge {

    @Id
    @Field("_id")
    private String id;

    /** The source node identifier */
    @NotNull private String sourceNodeId;

    /** The target node identifier */
    @NotNull private String targetNodeId;

    /** Optional bindings to map outputs from source to inputs on target */
    private Map<String, String> bindings;

    /**
     * Optional condition for activating this transition.
     * If null, the edge is considered unconditionally valid.
     */
    private Condition condition;

    /**
     * Represents a simple equality condition on a port's value
     */
    @Data
    public static class Condition {
        private String port;
        private String expectedValue;
    }
}
