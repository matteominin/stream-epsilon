package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.operational.WorkflowNode;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowEngine {
    private final List<WorkflowNode> nodes;

    public WorkflowEngine(List<WorkflowNode> nodes) {
        this.nodes = nodes;
    }

    public Map<String, Object> execute(Map<String, Object> context) throws Exception {
        for (WorkflowNode node : nodes) {
            System.out.println("Processing node: " + node.getClass().getName());
            node.process(context);
            System.out.println("Current context: " + context);
        }
        return context;
    }
}
