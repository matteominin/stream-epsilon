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
})
public class Port {
    Port() {}

    public static PortBuilder builder() {
        return new PortBuilder();
    }

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
        REST
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
            if (portNode instanceof ObjectNode) ((ObjectNode) portNode).remove("portType");
            return mapper.writeValueAsString(portNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Port to JSON", e);
        }
    }



    /**
     * Port builder
     */
    public static class PortBuilder {

        private String key;
        private PortSchema schema;
        private Object defaultValue;

        private PortBuilder() {
        }

        public PortBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public PortBuilder withSchema(PortSchema schema) {
            this.schema = schema;
            return this;
        }

        public PortBuilder withDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Port build() {
            Port port = new Port();
            port.setKey(key);
            port.setSchema(schema);
            port.setDefaultValue(defaultValue);
            port.setPortType(Port.PortImplementationType.STANDARD);


            if (key == null || key.isEmpty())
                throw new IllegalStateException("Key must be specified");


            if (schema == null)
                throw new IllegalStateException("Schema must be specified");


            if (defaultValue != null && schema.isValidValue(defaultValue))
                throw new IllegalStateException("Default value is not valid for the schema");

            return port;
        }
    }
}