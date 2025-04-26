package org.caselli.cognitiveworkflow.operational.core;

import org.springframework.stereotype.Service;

@Service
class WorkflowManager {
    /*
    private final WorkflowCatalogService catalog;
    private final AgentRegistry registry;

    public WorkflowManager(WorkflowCatalogService catalog, AgentRegistry registry) {
        this.catalog = catalog;
        this.registry = registry;
    }

    public void runWorkflow(WorkflowInstance instance) {
        WorkflowDescriptor descriptor = catalog.get(instance.id);
        String currentNodeId = instance.startNodeId;
        Map<String, Object> context = new HashMap<>();

        while (currentNodeId != null) {
            WorkflowNodeBinding binding = instance.nodes.get(currentNodeId);
            NodeDescriptor nodeDesc = descriptor.nodes.get(currentNodeId);

            Node agent = registry.getAgent(nodeDesc.nodeType);

            Map<String, Object> inputs = new HashMap<>();
            for (var entry : binding.bindings.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String ref) {
                    inputs.put(entry.getKey(), context.get(ref));
                } else if (value instanceof Map constant && constant.containsKey("const")) {
                    inputs.put(entry.getKey(), constant.get("const"));
                }
            }

            Map<String, Object> outputs = agent.execute(inputs);
            outputs.forEach(context::put);

            currentNodeId = binding.routes != null ? binding.routes.get("next") : null;
        }

        System.out.println(\"Final Context: \" + context);
    }

     */
}