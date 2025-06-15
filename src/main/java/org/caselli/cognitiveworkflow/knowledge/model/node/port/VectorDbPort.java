package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VectorDbPort extends Port {
    private VectorDbPortRole role;

    public static VectorPortBuilder builder() {
        return new VectorPortBuilder();
    }

    /**
     * Roles that Vector Database Ports can have.
     */
    public enum VectorDbPortRole {
        INPUT_VECTOR,
        RESULTS,
        FIRST_RESULT
    }

    /**
     * Vector Port builder
     */
    public static class VectorPortBuilder extends AbstractPortBuilder<VectorDbPort, VectorPortBuilder> {
        private VectorDbPortRole role;

        public VectorPortBuilder withRole(VectorDbPortRole role) {
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
            port.setPortType(PortImplementationType.VECTOR_DB);
            port.setRole(role);
            return port;
        }
    }
}