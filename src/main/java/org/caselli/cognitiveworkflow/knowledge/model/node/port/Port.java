package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * Represents a named input or output port of a node, including its schema definition.
 */
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "portType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StandardPort.class, name = "STANDARD"),
        @JsonSubTypes.Type(value = RestPort.class, name = "REST"),
})
public class Port {
    /** The key of the port */
    @NotNull private String key;

    /** The type schema of the port */
    @NotNull private PortSchema schema;

    /**
     * The type of the port.
     * Field for polymorphic deserialization
     */
    @NotNull private PortImplementationType portType;

    public enum PortImplementationType {
        STANDARD,
        REST,
        DB,
        LLM
    }
}