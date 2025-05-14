package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmbeddingsPort extends Port {
    private EmbeddingsPortRole role;

    /**
     * Roles that embeddings ports can have.
     */
    public enum EmbeddingsPortRole {
        INPUT_TEXT,
        OUTPUT_VECTOR,
    }
}