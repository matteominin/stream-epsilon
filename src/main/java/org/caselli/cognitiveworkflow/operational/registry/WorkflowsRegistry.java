package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowsRegistry {
    private final Map<String, WorkflowInstance> runningInstances = new ConcurrentHashMap<>();

    public Optional<WorkflowInstance> get(String workflowId) {
        return Optional.ofNullable(runningInstances.get(workflowId));
    }

    public void put(String workflowId, WorkflowInstance instance) {
        runningInstances.put(workflowId, instance);
    }

    public void remove(String workflowId) {
        runningInstances.remove(workflowId);
    }

    public void clear() {
        runningInstances.clear();
    }
}
