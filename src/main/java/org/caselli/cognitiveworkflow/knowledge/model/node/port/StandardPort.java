package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StandardPort extends Port {

    public static StandardPort.StandardPortBuilder builder() {
        return new StandardPort.StandardPortBuilder();
    }

    /**
     * Standard Port builder
     */
    public static class StandardPortBuilder extends AbstractPortBuilder<StandardPort, StandardPortBuilder> {
        @Override
        protected StandardPortBuilder self() {
            return this;
        }

        @Override
        protected StandardPort createInstance() {
            StandardPort port = new StandardPort();
            port.setPortType(PortImplementationType.STANDARD);
            return port;
        }
    }
}
