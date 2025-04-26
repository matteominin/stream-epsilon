package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.registry.WorkflowsRegistry;
import org.caselli.cognitiveworkflow.operational.utils.TemperatureSampler;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Routing Manager:
 * Determines if an appropriate instance of the workflow exists to handle the requested execution.
 * - If an instance is available, it routes the request to it;
 * - If an instance is not available, it consults the MetaCatalog and verifies if there is an appropriate workflow;
 *      - If an instance exists, asks the WorkflowFactory to instantiate it; then routes the request to it;
 *      - If none exists, tries to combine nodes;
 *      - If this fails, it throws an exception.
 */
@Service
public class RoutingManager {
    private final WorkflowsRegistry workflowsRegistry;
    private final WorkflowFactory workflowFactory;
    private final WorkflowMetamodelService metamodelService;

    // Temperature parameter for controlling randomness of workflows selection
    private final double temperature = 0.8;

    // Number of workflows candidates to consider
    private final int candidatesCount = 5;


    public RoutingManager(
            WorkflowsRegistry workflowsRegistry,
            WorkflowFactory workflowFactory,
            WorkflowMetamodelService metamodelService
    ) {
        this.workflowsRegistry = workflowsRegistry;
        this.workflowFactory = workflowFactory;
        this.metamodelService = metamodelService;
    }

    /**
     * Routes a workflow execution request to an appropriate instance
     *
     * @param intentId The id of the intent to route
     * @return The ID of the workflow instance that will handle the request
     */
    public String routeWorkflowRequest(String intentId) {
        // Check if a running instance already exists
        List<WorkflowInstance> existingInstances = workflowsRegistry.findTopNHandlingIntent(intentId, candidatesCount);
        if (existingInstances != null && !existingInstances.isEmpty()){
            // Select best workflow based on score
            WorkflowInstance bestDefinition = TemperatureSampler.sapleSortedList(existingInstances, temperature);
            if(bestDefinition != null) return bestDefinition.getId();
        }

        // If no Workflow in memory can handle the intent:
        // Load workflow definition from catalog
        List<WorkflowMetamodel> definitions = metamodelService.findTopNHandlingIntent(intentId, candidatesCount);

        if (definitions != null && !definitions.isEmpty()) {
            // Select best workflow based on score
            WorkflowMetamodel bestDefinition = TemperatureSampler.sapleSortedList(definitions, temperature);

            // Instantiate the new workflow
            WorkflowInstance instance = workflowFactory.createInstance(bestDefinition);

            // Register its instance
            workflowsRegistry.register(bestDefinition.getName(), instance);

            return instance.getId();
        }

        // No pre-defined workflows are found in the SBOM.
        else {
            // Try to combine Nodes
            // TODO [...]

            // If all attempts fail, throw an exception
            throw new NoWorkflowAvailableException("No workflow available to handle intent: " + intentId);
        }
    }

    public static class NoWorkflowAvailableException extends RuntimeException {
        public NoWorkflowAvailableException(String message) {
            super(message);
        }
    }
}