package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
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


    /**
     * Create a new Workflow Instance Bean
     * @param metamodel The mata-model of the instance
     * @return Returns a new Workflow Instance
     */
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

    /**
     * Refreshes deprecated nodes in the workflow instance by attempting to re-create them.
     * @param workflowInstance The workflow instance to refresh
     */
    public void refreshDeprecatedNodes(WorkflowInstance workflowInstance) {
        List<NodeInstance> currentNodes = workflowInstance.getNodeInstances();
        List<NodeInstance> refreshedNodes = new ArrayList<>(currentNodes.size());
        boolean hasRefreshed = false;

        for (NodeInstance nodeInstance : currentNodes) {
            if (nodeInstance.isDeprecated()) {
                logger.info("Refreshing deprecated node {} in workflow {}", nodeInstance.getId(), workflowInstance.getId());

                // Create a fresh instance for the deprecated node
                // -> It's an ATTEMPT to create a new instance of the node, but it could
                //    return the same node if it's running. We are delegating this responsibility
                //    to the nodeInstanceManager
                NodeInstance freshNode = nodeInstanceManager.getOrCreate(nodeInstance.getId());
                refreshedNodes.add(freshNode);
                hasRefreshed = true;

                logger.debug("Replaced deprecated node {} with fresh instance", nodeInstance.getId());
            } else {
                // Keep the existing non-deprecated node
                refreshedNodes.add(nodeInstance);
            }
        }

        if (hasRefreshed) {
            // Update the workflow with the refreshed node list
            workflowInstance.setNodeInstances(refreshedNodes);

            logger.info("Workflow {} refreshed with {} nodes updated",
                    workflowInstance.getId(),
                    currentNodes.size() - refreshedNodes.size() +
                            (int) refreshedNodes.stream().filter(n -> currentNodes.stream()
                                    .noneMatch(old -> old.getId().equals(n.getId()) && !old.isDeprecated())).count());
        }
    }
}
