package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.workflow.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.registry.WorkflowsRegistry;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkflowInstanceManager {


    private final WorkflowsRegistry workflowsRegistry;
    private final WorkflowFactory workflowFactory;

    public WorkflowInstanceManager(WorkflowsRegistry workflowsRegistry, WorkflowFactory workflowFactory) {
        this.workflowsRegistry = workflowsRegistry;
        this.workflowFactory = workflowFactory;
    }


    /**
     * Get or create a Workflow instance by its meta-model.
     * If it does not exist, it creates it and registers it.
     * @param workflowMetamodel The metamodel of the workflow
     * @return The existing or newly created NodeInstance
     */
    public WorkflowInstance getOrCreate(WorkflowMetamodel workflowMetamodel) {
        // Check if the instance already exists
        var existing = workflowsRegistry.get(workflowMetamodel.getId());
        if(existing.isPresent()) return existing.get();

        // Instantiate the new workflow
        WorkflowInstance instance = workflowFactory.createInstance(workflowMetamodel);

        // Register its instance
        workflowsRegistry.register(instance.getId(), instance);

        return instance;
    }


    /**
     * Find the top N workflow instances that can handle the specified intent,
     * sorted by their score for the intent (highest first)
     *
     * @param intentId The ID of the intent to handle
     * @param n Number of instances to return
     * @return List of workflow instances that can handle the intent, sorted by score
     */
    public List<WorkflowInstance> findTopNHandlingIntent(String intentId, int n) {
        return workflowsRegistry.list().stream()
                .filter(instance -> instance.canHandleIntent(intentId))
                .sorted((instance1, instance2) -> {
                    // Get the score for this intent from each instance's definition
                    Double score1 = instance1.getScoreForIntent(intentId);
                    Double score2 = instance2.getScoreForIntent(intentId);
                    // Compare in reverse order (highest first)
                    return score2.compareTo(score1);
                })
                .limit(n)
                .collect(Collectors.toList());
    }

}
