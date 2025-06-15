package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LlmPort extends Port {

    public static LlmPort.LlmPortBuilder builder() {
        return new LlmPort.LlmPortBuilder();
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
    public static class LlmPortBuilder extends AbstractPortBuilder<LlmPort, LlmPort.LlmPortBuilder> {
        private LlmPort.LlmPortRole role;

        public LlmPort.LlmPortBuilder withRole(LlmPort.LlmPortRole role) {
            this.role = role;
            return this;
        }

        @Override
        protected LlmPort.LlmPortBuilder self() {
            return this;
        }

        @Override
        protected LlmPort createInstance() {
            var port = new LlmPort();
            port.setRole(role);
            port.setPortType(PortImplementationType.LLM);
            return port;
        }
    }
}