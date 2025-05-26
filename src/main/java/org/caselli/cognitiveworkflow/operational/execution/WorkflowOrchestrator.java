package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.services.InputMapperService;
import org.caselli.cognitiveworkflow.operational.LLM.services.IntentDetectionService;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(WorkflowOrchestrator.class);
    private final WorkflowExecutor workflowExecutor;
    private final InputMapperService inputMapperService;
    private final IntentDetectionService intentDetectionService;
    private final RoutingManager routingManager;
    private final IntentMetamodelService intentMetamodelService;

    public WorkflowOrchestrator(WorkflowExecutor workflowExecutor, InputMapperService inputMapperService, IntentDetectionService intentDetectionService, RoutingManager routingManager, IntentMetamodelService intentMetamodelService) {
        this.workflowExecutor = workflowExecutor;
        this.inputMapperService = inputMapperService;
        this.intentDetectionService = intentDetectionService;
        this.routingManager = routingManager;
        this.intentMetamodelService = intentMetamodelService;
    }

    /**
     * Orchestrates the complete workflow execution process for a given user request.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Detects intent from the user request</li>
     *   <li>Routes to the appropriate workflow based on the intent</li>
     *   <li>Starts workflow execution with mapped inputs</li>
     * </ol>
     *
     * @param request The user request to be processed
     * @throws RuntimeException if intent cannot be satisfied or no workflow is available
     */
    public Map<String, Object> orchestrateWorkflowExecution(String request){
        logger.info("Starting workflow orchestration for request: {}", request);

        var intentRes = intentDetectionService.detect(request);
        logger.info("Intent detection result: {}", intentRes);

        if(intentRes == null) {
            logger.error("Intent cannot be satisfied for request: {}", request);
            throw new RuntimeException("Intent cannot be satisfied.");
        }

        String intentId;

        if(intentRes.isNew()){
            // INTENT DO NOT EXISTS IN THE CATALOG
            // Creating the new intent...
            IntentMetamodel newIntentMetamodel = new IntentMetamodel();
            newIntentMetamodel.setName(intentRes.getIntentName());
            newIntentMetamodel.setAIGenerated(true);
            var newIntent = intentMetamodelService.create(newIntentMetamodel);
            intentId = newIntent.getId();
            logger.info("New intent created with name {} and ID {}", intentId, newIntent.getName());
        } else{
            intentId = intentRes.getIntentId();
            logger.info("Existing intent found with ID: {}", intentId);
        }

        if(intentId == null) {
            logger.error("Intent ID is null for request: {}", request);
            throw new RuntimeException("Intent cannot be satisfied.");
        }

        // ROUTING
        logger.info("Routing workflow for intent ID: {}", intentId);
        WorkflowInstance workflowInstance = routingManager.routeWorkflowRequest(intentId);
        if (workflowInstance == null) {
            logger.error("No workflow available to handle intent: {}", intentId);
            throw new RuntimeException("No workflow available to handle intent: " + "intentId");
        }
        logger.info("Successfully routed to workflow instance: {}", workflowInstance.getId());

        // EXECUTION
        var userVariables = intentRes.getUserVariables();
        userVariables.put("COMPLETE_USER_REQUEST", request); // TODO: find better way
        logger.debug("Starting workflow with variables: {}", userVariables);
        var context = startWorkflow(workflowInstance, userVariables);


        logger.debug("Workflow execution completed for request: {}", request);

        return extractOutputs(context, workflowInstance);
    }

    /**
     * Start the execution of a workflow. Maps unstructured variables to the input ports
     * @param workflowInstance The instance of the workflow to execute
     * @param variables The variables of the user
     * @return Returns the final execution context
     */
    private ExecutionContext startWorkflow(WorkflowInstance workflowInstance, Map<String,Object> variables) {
        logger.debug("Starting workflow instance: {}", workflowInstance.getId());

        // Get the entry points of the workflow
        Set<String> entryPointIDs = workflowInstance.getMetamodel().getEntryNodes();
        logger.debug("Found {} entry points for workflow", entryPointIDs.size());

        List<NodeMetamodel> entryPointMetamodels = entryPointIDs.stream()
                .map(id -> workflowInstance.getInstanceByWorkflowNodeId(id).getMetamodel())
                .collect(Collectors.toList());

        var inputMapping = inputMapperService.mapInput(variables, entryPointMetamodels);
        logger.debug("Input mapping result: {}", inputMapping);

        if(inputMapping == null) {
            logger.error("Workflow failed to start: no starting node can be found for variables: {}", variables);
            throw new RuntimeException("Workflow Failed to start: no starting node can be found.");
        }

        ExecutionContext context = inputMapping.getContext();

        String startingNodeId = entryPointIDs.stream()
                .filter(x -> Objects.equals(workflowInstance.getInstanceByWorkflowNodeId(x).getMetamodel().getId(), inputMapping.getStartingNode().getId()))
                .findFirst()
                .orElse(null);

        logger.info("Starting workflow execution at node ID: {}", startingNodeId);

        logger.debug("Obtained workflow executor for instance: {}", workflowInstance.getId());
        workflowExecutor.execute(workflowInstance, context, startingNodeId);

        return context;
    }

    /**
     * Extract from the context the output ports of all the exit points of the workflow
     * @param context The execution context
     * @param workflowInstance The instance of the workflow
     * @return Returns a map as a subset of the context limited to only the output of the exit points
     */
    private Map<String, Object> extractOutputs(ExecutionContext context, WorkflowInstance workflowInstance){

        Map<String, Object> res = new HashMap<>();

        var exitPointIDs = workflowInstance.getMetamodel().getExitNodes();
        List<NodeMetamodel> exitPointMetamodels = exitPointIDs.stream()
                .map(id -> workflowInstance.getInstanceByWorkflowNodeId(id).getMetamodel())
                .toList();

        for (var model : exitPointMetamodels){
            if(model.getOutputPorts() != null){
                for (var outputPort : model.getOutputPorts()){
                    if(outputPort.getKey() != null && context.get(outputPort.getKey()) != null)
                        res.put(outputPort.getKey(), context.get(outputPort.getKey()));
                }
            }
        }

        return res;
    }
}