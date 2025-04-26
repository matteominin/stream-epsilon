package org.caselli.cognitiveworkflow.knowledge.model.shared;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
public class PortSchema {
    private PortType type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PortSchema items;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PortSchema> properties;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object defaultValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean required;
}