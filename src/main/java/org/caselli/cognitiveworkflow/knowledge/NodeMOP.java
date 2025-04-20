package org.caselli.cognitiveworkflow.knowledge;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NodeMOP {

    private final NodeRegistry registry;

    public NodeMOP() {
        this.registry = new NodeRegistry();
    }

    public void register(WorkflowNodeDescriptor desc) {
        registry.register(desc);
    }

    public void updateConfig(String id, Map<String, Object> config) {
        registry.update(id, config);
    }

    public void remove(String id) {
        registry.remove(id);
    }

    public NodeRegistry getRegistry() {
        return registry;
    }
}