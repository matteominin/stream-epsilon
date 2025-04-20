package org.caselli.cognitiveworkflow.knowledge;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeRegistry {
    private final Map<String, WorkflowNodeDescriptor> descriptors = new ConcurrentHashMap<>();

    public void register(WorkflowNodeDescriptor desc) {
        descriptors.put(desc.getId(), desc);
    }

    public WorkflowNodeDescriptor get(String id) {
        return descriptors.get(id);
    }

    public Collection<WorkflowNodeDescriptor> list() {
        return descriptors.values();
    }

    public void update(String id, Map<String, Object> newConfig) {
        WorkflowNodeDescriptor desc = get(id);
        if (desc != null) {
            desc.getConfig().putAll(newConfig);
        }
    }

    public void remove(String id) {
        descriptors.remove(id);
    }
}