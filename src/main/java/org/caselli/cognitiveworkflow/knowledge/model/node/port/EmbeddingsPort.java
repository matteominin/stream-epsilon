package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmbeddingsPort extends Port {
    private EmbeddingsPortRole role;

    public static EmbeddingsPort.EmbeddingsPortBuilder builder() {
        return new EmbeddingsPort.EmbeddingsPortBuilder();
    }

    /**
     * Roles that embeddings ports can have.
     */
    public enum EmbeddingsPortRole {
        INPUT_TEXT,
        OUTPUT_VECTOR,
    }


    /**
     * Embeddings Port builder
     */
    public static class EmbeddingsPortBuilder extends AbstractPortBuilder<EmbeddingsPort, EmbeddingsPort.EmbeddingsPortBuilder> {
        private EmbeddingsPort.EmbeddingsPortRole role;

        public EmbeddingsPort.EmbeddingsPortBuilder withRole(EmbeddingsPort.EmbeddingsPortRole role) {
            this.role = role;
            return this;
        }

        @Override
        protected EmbeddingsPort.EmbeddingsPortBuilder self() {
            return this;
        }

        @Override
        protected EmbeddingsPort createInstance() {
            var port = new EmbeddingsPort();
            port.setPortType(PortImplementationType.EMBEDDINGS);
            port.setRole(role);
            return port;
        }
    }
}