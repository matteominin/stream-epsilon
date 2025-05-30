package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowFactory {
    private final NodeInstanceManager nodeInstanceManager;

    private final ApplicationContext context;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(WorkflowFactory.class);

    public WorkflowFactory(ApplicationContext context, NodeInstanceManager nodeInstanceManager) {
        this.nodeInstanceManager = nodeInstanceManager;
        this.context = context;
    }

    public WorkflowInstance createInstance(WorkflowMetamodel metamodel) {
        WorkflowInstance bean = context.getBean(WorkflowInstance.class);

        bean.setId(metamodel.getId());

        // Set the metamodel
        bean.setMetamodel(metamodel);

        // Get the instances of the nodes
        List<NodeInstance> nodeInstances = metamodel.getNodes().stream()
                .map(nodeMeta -> nodeInstanceManager.getOrCreate(nodeMeta.getNodeMetamodelId()))
                .toList();

        bean.setNodeInstances(nodeInstances);

        logger.info("Initializing workflow instance with ID: " + bean.getId() + " and nodes: " + nodeInstances.stream().map(NodeInstance::getId).toList());

        return bean;
    }

}
