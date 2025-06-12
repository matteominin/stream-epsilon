package org.caselli.cognitiveworkflow.operational.instances;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Getter
@Component
@Scope("prototype")
public class WorkflowInstance {
    @NotNull
    @Setter
    private String id;

    /** If the workflow is deprecated. If it is, when the last execution finishes it will be re-instanced **/
    @Setter @Getter private boolean isDeprecated;

    // Metamodel
    private WorkflowMetamodel metamodel;

    // Nodes
    private List<NodeInstance> nodeInstances;

    private final Map<String, NodeInstance> nodeInstancesMap = new HashMap<>();
    private final Map<String, WorkflowNode> workflowNodesMap = new HashMap<>();


    /**
     * Initializes the node maps after properties are set by Spring.
     * This ensures maps are populated regardless of how the bean is created.
     */
    @PostConstruct
    public void initializeMaps() {
        refreshNodeMaps();
    }

    /**
     * Setter for nodeInstances that also refreshes the node maps
     * @param nodeInstances The list of node instances
     */
    public void setNodeInstances(List<NodeInstance> nodeInstances) {
        this.nodeInstances = nodeInstances;
        refreshNodeMaps();
    }

    /**
     * Setter for metamodel that also refreshes the node maps
     * @param metamodel The workflow metamodel
     */
    public void setMetamodel(WorkflowMetamodel metamodel) {
        this.metamodel = metamodel;
        refreshNodeMaps();
    }


    /**
     * Check if the current instance of the workflow can handle an Intent by the intent Id.
     * @param intentId The id of the intent
     * @return Return true if the instance can run the intent
     */
    public boolean canHandleIntent(String intentId) {
        if (metamodel == null || metamodel.getHandledIntents() == null) return false;

        return metamodel.getHandledIntents().stream()
                .anyMatch(intent -> intent.getIntentId().equals(intentId));
    }

    /**
     * Helper method to get the score for a specific intent from the workflow instance
     * @param intentId The ID of the intent
     * @return The score for the intent, or 0.0 if not found
     */
    public Double getScoreForIntent(String intentId) {
        return metamodel.getHandledIntents().stream()
                .filter(intent -> intent.getIntentId().equals(intentId))
                .findFirst()
                .map(WorkflowMetamodel.WorkflowIntentCapability::getScore)
                .orElse(0.0);
    }

    /**
     * Gets a node instance by its ID
     * @param nodeId The ID of the node instance to retrieve
     * @return The NodeInstance or null if not found
     */
    public NodeInstance getNodeInstanceById(String nodeId) {
        return nodeInstancesMap.get(nodeId);
    }

    /**
     * Gets a workflow node by its ID
     * @param workflowNodeId The ID of the workflow node to retrieve
     * @return The WorkflowNode or null if not found
     */
    public WorkflowNode getWorkflowNodeById(String workflowNodeId) {
        return workflowNodesMap.get(workflowNodeId);
    }

    /**
     * Returns the instance of a node by the workflow-specific node ID
     * @param workflowNodeId The workflow node ID
     * @return The corresponding NodeInstance or null if not found
     */
    public NodeInstance getInstanceByWorkflowNodeId(String workflowNodeId) {
        WorkflowNode workflowNode = this.workflowNodesMap.get(workflowNodeId);
        if (workflowNode == null) return null;
        return this.nodeInstancesMap.get(workflowNode.getNodeMetamodelId());
    }

    /**
     * Refreshes the internal lookup maps for quick access to node instances and workflow nodes by their IDs.
     * This method is called automatically whenever the workflow structure changes or when the instance is initialized.
     */
    public void refreshNodeMaps() {
        // Clear existing maps to prevent stale data
        nodeInstancesMap.clear();
        workflowNodesMap.clear();

        // Populate the node instances map
        if (nodeInstances != null) {
            for (NodeInstance node : nodeInstances) {
                if (node != null && node.getId() != null) {
                    nodeInstancesMap.put(node.getId(), node);
                }
            }
        }

        // Populate the workflow nodes map
        if (metamodel != null && metamodel.getNodes() != null) {
            for (WorkflowNode node : metamodel.getNodes()) {
                if (node != null && node.getId() != null) {
                    workflowNodesMap.put(node.getId(), node);
                }
            }
        }
    }
}