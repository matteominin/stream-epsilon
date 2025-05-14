package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        @JsonSubTypes.Type(value = LlmPort.class, name = "LLM"),
        @JsonSubTypes.Type(value = VectorDbPort.class, name = "VECTOR_DB"),
        @JsonSubTypes.Type(value = EmbeddingsPort.class, name = "EMBEDDINGS"),
})
public class Port {
    Port() {}

    /** The key of the port */
    @NotNull private String key;

    /** The type schema of the port */
    @NotNull private PortSchema schema;


    /** Default value for the port */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object defaultValue;


    /**
     * The type of the port.
     * Field for polymorphic deserialization
     */
    @NotNull private PortImplementationType portType;


    public enum PortImplementationType {
        STANDARD,
        REST,
        LLM,
        EMBEDDINGS,
        VECTOR_DB
    }



    /**
     * Converts a Port object to JSON string representation.
     * @param port Port object to convert
     * @return JSON string representation of the Port object
     */
    public static String portToJson(Port port) {
        ObjectMapper mapper = new ObjectMapper();
        // Exclude null fields
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            JsonNode portNode = mapper.valueToTree(port);
            // Remove "portType"
            if (portNode instanceof ObjectNode)
            {
                ((ObjectNode) portNode).remove("portType");
                ((ObjectNode) portNode).remove("role");
            }
            return mapper.writeValueAsString(portNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Port to JSON", e);
        }
    }
}