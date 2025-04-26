package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.NodeInstance;
import org.caselli.cognitiveworkflow.operational.core.NodeFactory;
import org.springframework.stereotype.Component;


@Component
public class NodesRegistry extends InstancesRegistry<NodeInstance> {
    private final NodeFactory nodeFactory;

    public NodesRegistry(NodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
    }
}