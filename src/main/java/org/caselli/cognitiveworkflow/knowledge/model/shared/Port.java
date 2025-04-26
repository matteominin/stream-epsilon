package org.caselli.cognitiveworkflow.knowledge.model.shared;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class Port {
    @NotNull
    private String key;

    @NotNull
    private PortSchema schema;
}