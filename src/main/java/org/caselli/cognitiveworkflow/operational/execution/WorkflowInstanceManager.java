package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.event.WorkflowMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.registry.WorkflowsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class WorkflowInstanceManager {

    private final Logger logger = LoggerFactory.getLogger(WorkflowInstanceManager.class);

    private final ConcurrentHashMap<String, AtomicInteger> runningWorkflows = new ConcurrentHashMap<>();

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
        if(existing.isPresent()) {
            // There is an instance in the registry

            // If the instance is not deprecated
            if(!existing.get().isDeprecated()) return existing.get();


            // If it is deprecated but it is in execution
            if(isRunning(existing.get().getId())) return existing.get();

            // The existing node is deprecated and it is not in execution
            // Therefore, we can safely remove it from the registry
            workflowsRegistry.remove(workflowMetamodel.getId());
            // Then we can proceed as it was not found
        }

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

    /**
     * Mark a workflow as in execution
     * @param workflowId Workflow ID
     */
    public void markRunning(String workflowId) {
        runningWorkflows.compute(workflowId, (id, count) -> {
            if (count == null) return new AtomicInteger(1);
            count.incrementAndGet();
            return count;
        });
    }

    /**
     * Mark a workflow as no longer in execution
     * @param workflowId Workflow ID
     */
    public void markFinished(String workflowId) {
        runningWorkflows.computeIfPresent(workflowId, (id, count) -> {
            int newVal = count.decrementAndGet();
            if (newVal <= 0) return null;
            return count;
        });
    }

    /**
     * Check if a workflow in in execution
     * @param workflowId Id of the workflow
     * @return Returns true if there is at least one instance of the workflow that is being executed
     */
    public boolean isRunning(String workflowId) {
        return runningWorkflows.containsKey(workflowId);
    }

    /**
     * Listens for updates to the workflow metamodel and refreshes the node maps accordingly.
     * @param event The event containing the updated metamodel
     */
    @EventListener
    public void onMetaNodeUpdated(WorkflowMetamodelUpdateEvent event) {
        var id = event.metamodelId();

        // Search for the instance of the metamodel
        var instance = this.workflowsRegistry.get(id);
        if(instance.isPresent()){

            this.logger.info("Operation layer received metamodel update event: updating workflow instance for workflow {}", instance.get().getId());

            // Check the type of the update
            var isBreaking = false;
            // The update is braking if there is a version bump of the major version
            if(event.updatedMetamodel().getVersion().getMajor() - instance.get().getMetamodel().getVersion().getMajor() > 0) isBreaking = true;
            // The update is breaking if the nodes if the workflows were changed
            else if(WorkflowMetamodel.haveNodesChanged(event.updatedMetamodel(), instance.get().getMetamodel())) isBreaking = true;


            // If the workflow is running or the update is breaking, no hot-swapping
            if(isRunning(id) || isBreaking){
                // Re-Installation
                // We mark it as deprecated, when the last execution of this workflow finishes, it will be deleted
                // from the registry (forcing its update)
                instance.get().setDeprecated(true);

            } else {
                // HOT-SWAP
                // Directly update the metamodel
                instance.get().setMetamodel(event.updatedMetamodel());
                // Refresh the node maps
                instance.get().refreshNodeMaps();
            }
        }
    }
}
