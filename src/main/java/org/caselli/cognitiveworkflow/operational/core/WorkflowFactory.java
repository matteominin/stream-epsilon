package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.springframework.stereotype.Component;

@Component
public class WorkflowFactory {
    private final WorkflowMetamodelCatalog catalog;

    public WorkflowFactory(WorkflowMetamodelCatalog catalog) {
        this.catalog = catalog;
    }

    public WorkflowInstance createInstance(WorkflowMetamodel metamodel){
        return null; // TODO
    }

/*


TODO remove


    public WorkflowInstance createFromDescriptor(String descriptorId) {
        WorkflowDescriptor desc = catalog.findById(descriptorId).orElseThrow();

        WorkflowInstance instance = new WorkflowInstance();
        instance.id = descriptorId;
        instance.startNodeId = desc.startNodeId;

        desc.nodes.forEach((nodeId, nodeDesc) -> {
            WorkflowNodeBinding binding = new WorkflowNodeBinding();
            binding.bindings = new HashMap<>(nodeDesc.defaultBindings);
            binding.routes = new HashMap<>(nodeDesc.routes);
            instance.nodes.put(nodeId, binding);
        });

        return instance;
    }*/
}
