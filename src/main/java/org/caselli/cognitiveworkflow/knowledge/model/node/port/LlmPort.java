package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LlmPort extends Port {

    public static LlmPort.LLMPortBuilder LLMBuilder() {
        return new LlmPort.LLMPortBuilder();
    }


    /**
     * REST-specific port roles
     */
    private LlmPortRole role;


    /**
     * REST-specific port roles
     */
    public enum LlmPortRole {
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
        private LlmPortRole role;

        private LLMPortBuilder() {}

        public LlmPort.LLMPortBuilder withKey(String key) {
            this.key = key;
            return this;
        }

        public LlmPort.LLMPortBuilder withSchema(PortSchema schema) {
            this.schema = schema;
            return this;
        }

        public LlmPort.LLMPortBuilder withDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public LlmPort.LLMPortBuilder withRole(LlmPortRole role) {
            this.role = role;
            return this;
        }

        public LlmPort build() {
            LlmPort port = new LlmPort();
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