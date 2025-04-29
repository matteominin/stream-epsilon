package org.caselli.cognitiveworkflow.knowledge.model.shared;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * Represents a named input or output port of a node, including its schema definition.
 */
@Data
public class Port {
    @NotNull
    private String key;

    @NotNull
    private PortSchema schema;
}