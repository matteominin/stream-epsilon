package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VectorDbPort extends Port {
    private VectorSearchPortRole role;

    /**
     * Roles that Vector Database Ports can have.
     */
    public enum VectorSearchPortRole {
        INPUT_VECTOR,
        RESULTS,
        FIRST_RESULT
    }
}