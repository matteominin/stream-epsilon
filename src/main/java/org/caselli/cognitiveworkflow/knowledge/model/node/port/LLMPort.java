package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LLMPort extends Port {

    public static LLMPort.LLMPortBuilder LLMBuilder() {
        return new LLMPort.LLMPortBuilder();
    }


    /**
     * REST-specific port roles
     */
    private LLMPortRole role;


    /**
     * REST-specific port roles
     */
    public enum LLMPortRole {
        USER_PROMPT,
        SYSTEM_PROMPT_VARIABLE,
        RESPONSE
    }


    /**
     * LLM Port builder
     */
    public static class LLMPortBuilder {

        private String key;
        private PortSchema schema;
        private Object defaultValue;
        private LLMPort.LLMPortRole role;

        private LLMPortBuilder() {}

        public LLMPort.LLMPortBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public LLMPort.LLMPortBuilder withSchema(PortSchema schema) {
            this.schema = schema;
            return this;
        }

        public LLMPort.LLMPortBuilder withDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public LLMPort.LLMPortBuilder withRole(LLMPort.LLMPortRole role) {
            this.role = role;
            return this;
        }

        public LLMPort build() {
            LLMPort port = new LLMPort();
            port.setKey(key);
            port.setSchema(schema);
            port.setDefaultValue(defaultValue);
            port.setPortType(PortImplementationType.LLM);
            port.setRole(role);

            if (key == null || key.isEmpty()) throw new IllegalStateException("Key must be specified");
            if (schema == null) throw new IllegalStateException("Schema must be specified");
            if (defaultValue != null && schema.isValidValue(defaultValue)) throw new IllegalStateException("Default value is not valid for the schema");

            return port;
        }
    }
}