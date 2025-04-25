package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEngine {
    private final List<WorkflowInstance> nodes;

    public WorkflowEngine(List<WorkflowInstance> nodes) {
        this.nodes = nodes;
    }

    public void execute(Map<String, Object> context) {

    }
}
