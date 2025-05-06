package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class RestPort extends Port {

    /** Defines the role this port plays in REST context */
    @NotNull private RestPortRole role;

    public RestPort() {
        this.setPortType(PortImplementationType.REST);
    }

    public static RestPortBuilder resBuilder() {
        return new RestPortBuilder();
    }

    /**
     * REST-specific port roles
     */
    public enum RestPortRole {
        REQ_BODY_FIELD,
        REQ_BODY,
        REQ_HEADER,
        REQ_HEADER_FIELD,
        REQ_PATH_VARIABLE,
        REQ_QUERY_PARAMETER,
        RES_FULL_BODY,
        RES_BODY_FIELD,
        RES_STATUS,
        RES_HEADERS,
    }

    /**
     * Rest Port builder
     */
    public static class RestPortBuilder {

        private String key;
        private PortSchema schema;
        private Object defaultValue;
        private RestPortRole role;

        private RestPortBuilder() {
        }

        public RestPortBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public RestPortBuilder withSchema(PortSchema schema) {
            this.schema = schema;
            return this;
        }

        public RestPortBuilder withDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public RestPortBuilder withRole(RestPortRole role) {
            this.role = role;
            return this;
        }

        public RestPort build() {
            RestPort port = new RestPort();
            port.setKey(key);
            port.setSchema(schema);
            port.setDefaultValue(defaultValue);
            port.setPortType(PortImplementationType.REST);
            port.setRole(role);


            if (key == null || key.isEmpty())
                throw new IllegalStateException("Key must be specified");


            if (schema == null)
                throw new IllegalStateException("Schema must be specified");


            if (defaultValue != null && !schema.isValidValue(defaultValue))
                throw new IllegalStateException("Default value is not valid for the schema");

            return port;
        }
    }
}
