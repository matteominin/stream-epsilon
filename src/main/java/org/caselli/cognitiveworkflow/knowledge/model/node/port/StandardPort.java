package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StandardPort extends Port {

    public StandardPort() {
        this.setPortType(PortImplementationType.STANDARD);
    }
}