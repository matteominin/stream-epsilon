package org.caselli.cognitiveworkflow.operational;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;

@Data
public class WorkflowInstance {
    public String id;

    // Metamodel
    private WorkflowMetamodel metamodel;

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
     * @param instance The workflow instance
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
}

/*

TODO

package org.caselli.cognitiveworkflow.operational;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowEdge;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class WorkflowInstance {
    // Core Identity
    private String id;
    private String workflowType;  // Refers to the name of the WorkflowMetamodel

    // Reference to the definition
    private WorkflowMetamodel definition;

    // Runtime State
    private Map<String, WorkflowNodeInstance> nodeInstances;
    private List<WorkflowEdge> activeEdges;
    private Map<String, Object> contextData;
    private ExecutionState state;

    // Metadata
    private Date createdAt;
    private Date lastUpdatedAt;
    private Date completedAt;

    // Constructors
    public WorkflowInstance() {
        this.id = UUID.randomUUID().toString();
        this.contextData = new HashMap<>();
        this.nodeInstances = new HashMap<>();
        this.state = ExecutionState.INITIALIZED;
        this.createdAt = new Date();
    }

    public WorkflowInstance(WorkflowMetamodel definition) {
        this();
        this.definition = definition;
        this.workflowType = definition.getName();
        this.activeEdges = definition.getEdges();

        // Instantiate all nodes from the metamodel
        definition.getNodes().forEach(node -> {
            WorkflowNodeInstance nodeInstance = new WorkflowNodeInstance(node.getNodeId());
            nodeInstance.setConfigurationOverrides(node.getConfigurationOverrides());
            nodeInstances.put(node.getNodeId(), nodeInstance);
        });
    }

    // Intent handling
    public boolean canHandleIntent(String intentId) {
        if (definition == null || definition.getHandledIntents() == null) {
            return false;
        }

        return definition.getHandledIntents().stream()
                .anyMatch(intent -> intent.getIntentId().equals(intentId));
    }

    // Workflow execution methods
    public void start() {
        this.state = ExecutionState.RUNNING;
        this.lastUpdatedAt = new Date();
    }

    public void complete() {
        this.state = ExecutionState.COMPLETED;
        this.completedAt = new Date();
        this.lastUpdatedAt = new Date();
    }

    public void fail(String reason) {
        this.state = ExecutionState.FAILED;
        this.contextData.put("failureReason", reason);
        this.lastUpdatedAt = new Date();
    }

    public void pause() {
        this.state = ExecutionState.PAUSED;
        this.lastUpdatedAt = new Date();
    }

    public void resume() {
        if (this.state == ExecutionState.PAUSED) {
            this.state = ExecutionState.RUNNING;
            this.lastUpdatedAt = new Date();
        }
    }

    // Inner classes
    @Data
    public static class WorkflowNodeInstance {
        private String nodeId;
        private Map<String, Object> configurationOverrides;
        private ExecutionState state;
        private Date startedAt;
        private Date completedAt;
        private Object result;

        public WorkflowNodeInstance(String nodeId) {
            this.nodeId = nodeId;
            this.state = ExecutionState.INITIALIZED;
            this.configurationOverrides = new HashMap<>();
        }
    }

    public enum ExecutionState {
        INITIALIZED,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED
    }
}


 */