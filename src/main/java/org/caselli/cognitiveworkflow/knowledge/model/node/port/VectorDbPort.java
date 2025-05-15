package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VectorDbPort extends Port {
    private VectorSearchPortRole role;

    public static VectorPortBuilder builder() {
        return new VectorPortBuilder();
    }

    /**
     * Roles that Vector Database Ports can have.
     */
    public enum VectorSearchPortRole {
        INPUT_VECTOR,
        RESULTS,
        FIRST_RESULT
    }

    /**
     * Vector Port builder
     */
    public static class VectorPortBuilder extends AbstractPortBuilder<VectorDbPort, VectorPortBuilder> {
        private VectorDbPort.VectorSearchPortRole role;

        public VectorPortBuilder withRole(VectorDbPort.VectorSearchPortRole role) {
            this.role = role;
            return this;
        }

        @Override
        protected VectorPortBuilder self() {
            return this;
        }

        @Override
        protected VectorDbPort createInstance() {
            var port = new VectorDbPort();
            port.setRole(role);
            return port;
        }
    }
}