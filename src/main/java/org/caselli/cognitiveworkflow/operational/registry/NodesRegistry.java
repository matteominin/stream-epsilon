package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class NodesRegistry extends InstancesRegistry<WorkflowNode> {

    public void update(String id, Map<String, Object> newConfig) {
        // TODO: Implement based on commented code in original class

    }
}