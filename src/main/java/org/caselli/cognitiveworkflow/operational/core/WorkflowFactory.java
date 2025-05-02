package org.caselli.cognitiveworkflow.operational.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.node.NodeInstance;
import org.caselli.cognitiveworkflow.operational.workflow.WorkflowInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowFactory {
    private final NodeInstanceManager nodeInstanceManager;

    private final ApplicationContext context;
    private final ObjectMapper mapper;

    public WorkflowFactory(ApplicationContext context, ObjectMapper mapper, NodeInstanceManager nodeInstanceManager) {
        this.nodeInstanceManager = nodeInstanceManager;
        this.context = context;
        this.mapper = mapper;
    }

    public WorkflowInstance createInstance(WorkflowMetamodel metamodel) {
        WorkflowInstance bean = context.getBean(WorkflowInstance.class);

        // TODO; inject id (separate instance id and metamodel id)
        bean.setId(metamodel.getId());

        // TODO: inject config
        // mapper.updateValue(node, metamodel.getConfig());

        // TODO: Copy the static fields
        // node.setId(desc.getId());
        // node.setInputKeys(desc.getInputKeys());
        // node.setOutputKeys(desc.getOutputKeys());

        // Set the metamodel
        bean.setMetamodel(metamodel);

        // Get the instances of the nodes
        List<NodeInstance> nodeInstances = metamodel.getNodes().stream()
                .map(nodeMeta -> {
                    return nodeInstanceManager.getOrCreate(nodeMeta.getNodeMetamodelId());
                })
                .toList();

        bean.setNodeInstances(nodeInstances);

        // TODO: print bean
        System.out.println("Created workflow bean=" + bean + " with id=" + bean.getId());

        return bean;
    }
}
