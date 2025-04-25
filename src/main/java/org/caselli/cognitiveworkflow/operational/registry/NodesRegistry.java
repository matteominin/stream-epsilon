package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.WorkflowNode;
import java.util.Map;

public class NodesRegistry extends InstancesRegistry<WorkflowNode> {

    public void update(String id, Map<String, Object> newConfig) {
        // TODO: Implement based on commented code in original class
        /*
        WorkflowNode node = get(id);
        if (node != null) {
            node.getConfig().putAll(newConfig);
        }
        */
    }
}