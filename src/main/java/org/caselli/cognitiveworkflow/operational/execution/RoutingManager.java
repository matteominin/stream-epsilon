package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.utils.TemperatureSampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Routing Manager:
 * Determines if an appropriate instance of the workflow exists to handle the requested execution.
 * - If an instance is available, it routes the request to it;
 * - If an instance is not available, it consults the MetaCatalog and verifies if there is an appropriate workflow;
 *      - If an instance exists, asks the WorkflowInstanceManager to instantiate it; then routes the request to it;
 *      - If none exists, tries to combine nodes;
 *      - If this fails, it throws an exception.
 */
@Service
public class RoutingManager {
    private final Logger logger = LoggerFactory.getLogger(RoutingManager.class);
    private final WorkflowInstanceManager workflowInstanceManager;
    private final WorkflowMetamodelService metamodelService;

    // Temperature parameter for controlling randomness of workflows selection
    private final double temperature = 0.8;

    // Number of workflows candidates to consider
    private final int candidatesCount = 5;


    public RoutingManager(
            WorkflowInstanceManager workflowInstanceManager,
            WorkflowMetamodelService metamodelService
    ) {
        this.workflowInstanceManager = workflowInstanceManager;
        this.metamodelService = metamodelService;
    }

    /**
     * Routes a workflow execution request to an appropriate instance
     * @param intentId The id of the intent to route
     * @return The ID of the workflow instance that will handle the request
     */
    public WorkflowInstance routeWorkflowRequest(String intentId) {
        // Check if a running instance already exists
        List<WorkflowInstance> existingInstances = workflowInstanceManager.findTopNHandlingIntent(intentId, candidatesCount);
        if (existingInstances != null && !existingInstances.isEmpty()){
            // Select best workflow based on score
            WorkflowInstance bestDefinition = TemperatureSampler.sapleSortedList(existingInstances, temperature);
            if(bestDefinition != null) {

                logger.info("Running instance for workflow handling the intent with ID {} was found", intentId);

                // Instead of directly return the instance, we call the workflowInstanceManager
                // to get the instance of that metamodel
                // This is fundamental as the method 'getOrCreate' check if the instance is marked as 'deprecated'
                // as in case it is and it is not running, it re-creates it with the new metamodel version.
                // Therefore, without calling 'getOrCreate', deprecated version are never removed
                // (otherwise, we could create a sort of garbage collector scheduled to run periodically)
                return this.workflowInstanceManager.getOrCreate(bestDefinition.getMetamodel());
            }
        }

        logger.info("No running instance for workflow handling the intent with ID {} was found", intentId);

        // If no Workflow in memory can handle the intent:
        // Load workflow definition from catalog
        List<WorkflowMetamodel> candidates = metamodelService.findTopNHandlingIntent(intentId, candidatesCount);

        if (candidates != null && !candidates.isEmpty()) {
            // Select best workflow based on score
            // For now we are using a temperature-based sampling technique in order to
            // avoid to select always best workflow
            // TODO: A future improvement could be to implement the routing selection as an LLM agent that select the best
            // workflow not only for its score, but also for its capabilities and metadata
            WorkflowMetamodel bestDefinition = TemperatureSampler.sapleSortedList(candidates, temperature);

            logger.info("Found a Workflow Metamodel handling the intent with ID {}. Creating a new instance", intentId);

            // Instantiate the new workflow
            return workflowInstanceManager.getOrCreate(bestDefinition);
        }

        // No pre-defined workflows are found in the catalog.
        else {
            // Try to combine Nodes
            // TODO: Workflow Synthesis

            logger.info("No pre-existing workflow metamodels can handle the intent with ID {}", intentId);

            // If all attempts fail, returns null
            return null;
        }
    }
}