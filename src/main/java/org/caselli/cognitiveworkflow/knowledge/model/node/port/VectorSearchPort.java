package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VectorSearchPort extends Port {
    private VectorSearchPortRole role;

    /**
     * Roles that vector search ports can have.
     */
    public enum VectorSearchPortRole {
        QUERY_TEXT,
        RESULTS,
    }
}