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
import java.util.List;
import java.util.Optional;


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


    /**
     * Helper method to retrieve and resolve a PortSchema given a full path (e.g., "basePort.nestedField").
     * @param ports Available ports to search in
     * @param fullPathKey The dot-notation path to the port field
     * @return Return the schema of the port field corresponding to the nested path
     */
    public static PortSchema getResolvedSchemaForPort(List<? extends Port> ports, String fullPathKey) {
        if (fullPathKey == null || fullPathKey.isEmpty() || ports == null || ports.isEmpty()) return null;

        // Split into base port key and the rest of the path (if any)
        // "port.field.sub" -> parts[0]="port", parts[1]="field.sub"
        String[] parts = fullPathKey.split("\\.", 2);
        String basePortKey = parts[0];
        String nestedPath = (parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) ? parts[1] : null;

        if (basePortKey.isEmpty()) return null;

        Optional<? extends Port> basePortOpt = ports.stream()
                .filter(p -> p.getKey() != null && p.getKey().equals(basePortKey))
                .findFirst();

        if (basePortOpt.isEmpty()) return null;

        Port basePort = basePortOpt.get();
        PortSchema baseSchema = basePort.getSchema();

        if (baseSchema == null) return null;

        if (nestedPath != null) {
            try {
                return baseSchema.getSchemaByPath(nestedPath);
            } catch (Exception e) {
                return null;
            }
        } else {
            return baseSchema;
        }
    }

}