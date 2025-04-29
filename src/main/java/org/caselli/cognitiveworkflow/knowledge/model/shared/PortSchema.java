package org.caselli.cognitiveworkflow.knowledge.model.shared;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * Defines the data type and structure of a port, supporting nested schemas for objects and arrays.
 */
@Data
public class PortSchema {
    /** Basic data type of the port (e.g. STRING, BOOLEAN, etc.) */
    private PortType type;

    /** Schema for items if the port is an array */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PortSchema items;

    /** Schema for properties if the port is an object */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PortSchema> properties;

    /** Default value for the port */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object defaultValue;

    /** Whether the port is required for execution */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean required;
}
