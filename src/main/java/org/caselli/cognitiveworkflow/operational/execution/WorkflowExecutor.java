package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.AI.services.PortAdapterService;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.observability.NodeObservabilityReport;
import org.caselli.cognitiveworkflow.operational.observability.TokenUsage;
import org.caselli.cognitiveworkflow.operational.observability.WorkflowObservabilityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Class for executing workflows with comprehensive observability.
 * Execution progresses in a topological order.
 * @author niccolocaselli
 */
@Service
public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final WorkflowMetamodelService workflowMetamodelService;
    private final PortAdapterService portAdapterService;
    private final WorkflowInstanceManager workflowInstanceManager;
    private final NodeInstanceManager nodeInstanceManager;
    private final EdgeConditionEvaluator edgeConditionEvaluator;

    public WorkflowExecutor(WorkflowMetamodelService workflowMetamodelService,
                            PortAdapterService portAdapterService,
                            WorkflowInstanceManager workflowInstanceManager,
                            NodeInstanceManager nodeInstanceManager, EdgeConditionEvaluator edgeConditionEvaluator) {

        this.workflowMetamodelService = workflowMetamodelService;
        this.portAdapterService = portAdapterService;
        this.workflowInstanceManager = workflowInstanceManager;
        this.nodeInstanceManager = nodeInstanceManager;
        this.edgeConditionEvaluator = edgeConditionEvaluator;
    }

    /**
     * Executes a workflow
     * @param workflow The workflow instance to execute
     * @param context The execution context
     * @return Detailed execution result with performance metrics and observability data
     */
    public WorkflowObservabilityReport execute(WorkflowInstance workflow, ExecutionContext context) {
        // Observability
        WorkflowObservabilityReport executionRecord = new WorkflowObservabilityReport(
                workflow.getId(),
                workflow.getMetamodel().getName(),
                context
        );

        try {
            logger.info("-------------------------------------------");
            logger.info("Starting workflow execution: {} (ID: {})", executionRecord.getWorkflowName(), workflow.getId());

            // Mark the workflow as in execution
            workflowInstanceManager.markRunning(workflow.getId());

            // Check if the workflow is enabled
            if (!workflow.getMetamodel().getEnabled()) {
                String errorMsg = "Cannot execute Workflow " + workflow.getId() + ". It is not enabled.";
                executionRecord.markCompleted(false, errorMsg, new RuntimeException(errorMsg));
                throw new RuntimeException(errorMsg);
            }

            // Validate that all nodes referenced in edges exist
            List<WorkflowEdge> edges = workflow.getMetamodel().getEdges();
            validateEdges(workflow, edges);

            // Build execution state tracking
            ExecutionState executionState = buildExecutionState(workflow, edges);

            // Process nodes with proper MERGE/JOIN semantics
            processWorkflowNodes(workflow, context, executionRecord, executionState);


            // Mark workflow as successfully completed
            executionRecord.markCompleted(true, null, null);

            logger.info("-------------------------------------------");
            logger.info("Workflow execution completed successfully");
            logger.info("Total execution time: {} ms", executionRecord.getTotalExecutionTime().toMillis());
            logger.info("{} total nodes, {} successful, {} failed",
                    executionRecord.getMetrics().getTotalNodes(),
                    executionRecord.getMetrics().getSuccessfulNodes(),
                    executionRecord.getMetrics().getFailedNodes());
            logger.info("-------------------------------------------");

            return executionRecord;

        } catch (Exception e) {
            if (executionRecord.isSuccess()) executionRecord.markCompleted(false, e.getMessage(), e);
            throw e;

        } finally {
            // Mark the workflow as no longer in execution
            workflowInstanceManager.markFinished(workflow.getId());
        }
    }

    /**
     * Processes outgoing edges from a node
     */
    private void processOutgoingEdges(WorkflowInstance workflow, String sourceNodeId, ExecutionContext context, WorkflowObservabilityReport executionRecord, ExecutionState executionState) {

        List<WorkflowEdge> outgoingEdges = executionState.outgoingEdges.getOrDefault(sourceNodeId, Collections.emptyList());

        for (WorkflowEdge edge : outgoingEdges) {
            String targetNodeId = edge.getTargetNodeId();
            NodeExecutionState targetState = executionState.nodeStates.get(targetNodeId);

            if (targetState == null) {
                logger.warn("Target node {} not found for edge {}", targetNodeId, edge.getId());
                continue;
            }

            // Evaluate edge condition
            boolean conditionPassed = evaluateEdgeCondition(edge, context);

            Map<String, String> appliedBindings = null;
            if (conditionPassed) {
                // Apply bindings
                if (edge.getBindings() != null) {
                    appliedBindings = new HashMap<>(edge.getBindings());
                    applyEdgeBindings(workflow, edge, context);
                }

                // Mark this incoming edge as satisfied
                targetState.satisfiedIncomingEdges++;

                logger.info("Edge condition passed: {} -> {} (satisfied: {}/{})", sourceNodeId, targetNodeId, targetState.satisfiedIncomingEdges, targetState.totalIncomingEdges);
            } else {
                logger.info("Edge condition failed: {} -> {}", sourceNodeId, targetNodeId);
            }

            // Record edge evaluation
            executionRecord.recordEdgeEvaluation(sourceNodeId, targetNodeId, edge.getId(), conditionPassed, conditionPassed ? "Condition passed" : "Condition not met", appliedBindings);

            // Check if target node is ready to execute based on its execution type
            boolean targetReady = isNodeReadyToExecute(targetState);

            if (targetReady && !executionState.readyQueue.contains(targetNodeId)) {
                executionState.readyQueue.add(targetNodeId);
                logger.info("Node {} is now ready for execution (type: {})", targetNodeId, targetState.executionType);
            }
        }
    }


    /**
     * Determines if a node is ready to execute based on its execution type
     */
    private boolean isNodeReadyToExecute(NodeExecutionState nodeState) {
        return switch (nodeState.executionType) {
            case MERGE ->
                // MERGE nodes execute when at least one incoming edge is satisfied
                    nodeState.satisfiedIncomingEdges > 0;
            default ->
                // JOIN nodes execute when all incoming edges are satisfied
                    nodeState.satisfiedIncomingEdges >= nodeState.totalIncomingEdges;
        };
    }


    /**
     * Builds the initial execution state for the workflow
     */
    private ExecutionState buildExecutionState(WorkflowInstance workflow, List<WorkflowEdge> edges) {
        ExecutionState state = new ExecutionState();

        // Initialize node states
        for (String nodeId : workflow.getWorkflowNodesMap().keySet()) {
            NodeInstance nodeInstance = workflow.getInstanceByWorkflowNodeId(nodeId);
            WorkflowNode workflowNode = workflow.getWorkflowNodesMap().get(nodeId);

            NodeExecutionState nodeState = new NodeExecutionState();
            nodeState.nodeId = nodeId;
            nodeState.nodeInstance = nodeInstance;
            nodeState.executionType = workflowNode.getExecutionType();
            nodeState.totalIncomingEdges = 0;
            nodeState.satisfiedIncomingEdges = 0;
            nodeState.hasIncomingEdges = false;

            state.nodeStates.put(nodeId, nodeState);
        }

        // Build adjacency lists and count incoming edges
        for (WorkflowEdge edge : edges) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            state.outgoingEdges.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(edge);

            NodeExecutionState targetState = state.nodeStates.get(targetId);
            if (targetState != null) {
                targetState.totalIncomingEdges++;
                targetState.hasIncomingEdges = true;
            }
        }

        // Initialize ready queue with nodes that have no incoming edges
        for (NodeExecutionState nodeState : state.nodeStates.values()) {
            if (!nodeState.hasIncomingEdges) {
                state.readyQueue.add(nodeState.nodeId);
                logger.info("Node {} has no incoming edges, adding to ready queue", nodeState.nodeId);
            }
        }

        logger.info("Execution state initialized: {} nodes, {} ready initially", state.nodeStates.size(), state.readyQueue.size());

        return state;
    }


    /**
     * Executes a single node
     */
    private void executeNode(WorkflowInstance workflow, String workflowNodeId, NodeInstance nodeInstance, ExecutionContext context, WorkflowObservabilityReport executionRecord) throws Exception {

        try {
            // Apply default values for inputs
            prepareNodeInputs(nodeInstance, context);

            // Check required inputs
            ensureRequiredInputsSatisfied(workflow, workflowNodeId, context, executionRecord);

            logger.info("Current context keys: {}", context.keySet());

            // Mark node as running
            nodeInstanceManager.markRunning(nodeInstance.getId());

            // Node Execution Observability
            NodeObservabilityReport nodeObservabilityReport = new NodeObservabilityReport(
                    nodeInstance.getId(),
                    workflow.getId()
            );

            // Execute the node instance
            nodeInstance.process(context, nodeObservabilityReport);

            // Check token usage for observability
            if(!nodeObservabilityReport.getTokenUsage().isEmpty())
                executionRecord.recordTokenUsage(nodeObservabilityReport.getTokenUsage());

        } finally {
            nodeInstanceManager.markFinished(nodeInstance.getId());
        }

        // Apply default output values
        applyDefaultOutputValues(nodeInstance, context);
    }


    /**
     * Processes workflow nodes
     */
    private void processWorkflowNodes(WorkflowInstance workflow, ExecutionContext context, WorkflowObservabilityReport executionRecord, ExecutionState executionState) {

        Set<String> processedNodes = new HashSet<>();

        while (!executionState.readyQueue.isEmpty()) {
            String currentId = executionState.readyQueue.poll();
            NodeExecutionState currentState = executionState.nodeStates.get(currentId);

            if (processedNodes.contains(currentId)) {
                logger.debug("Node {} already processed, skipping", currentId);
                continue;
            }

            logger.info("*******************************************");
            logger.info("Processing node: {} (type: {})", currentId, currentState.executionType);

            // Record node execution start
            executionRecord.recordNodeStart(
                    currentId,
                    currentState.nodeInstance.getMetamodel().getName() != null ?
                            currentState.nodeInstance.getMetamodel().getName() : "Unnamed Node",
                    currentState.nodeInstance.getMetamodel().getClass().getSimpleName(),
                    context
            );

            boolean nodeExecutionSuccessful = false;
            String nodeErrorMessage = null;
            Throwable nodeException = null;

            try {

                // Execute the node
                executeNode(workflow, currentState.nodeId, currentState.nodeInstance, context, executionRecord);
                nodeExecutionSuccessful = true;
                processedNodes.add(currentId);

                logger.info("Node {} executed successfully", currentId);

            } catch (Exception e) {
                nodeErrorMessage = e.getMessage();
                nodeException = e;
                logger.error("Error processing node {}: {}", currentId, e.getMessage(), e);

                // For failed nodes, we still need to evaluate outgoing edges to potentially
                // unblock downstream nodes (especially MERGE nodes)
                // TODO
            }

            // Record node completion
            executionRecord.recordNodeCompletion(currentId, nodeExecutionSuccessful, nodeErrorMessage, nodeException, context);

            // Process outgoing edges regardless of node execution success
            processOutgoingEdges(workflow, currentId, context, executionRecord, executionState);

            // If node failed and it's critical, we might want to stop execution
            if (!nodeExecutionSuccessful) {
                // You can add logic here to determine if this failure should stop the workflow
                // For now, we continue to allow downstream MERGE nodes to potentially execute
                logger.warn("Node {} failed, but continuing workflow execution", currentId);
            }
        }
    }


    /**
     * Evaluates the condition on an edge to determine if execution should proceed.
     * @param edge The edge
     * @param context The current context
     * @return true if the condition passes or there is no condition, false otherwise
     */
    private boolean evaluateEdgeCondition(WorkflowEdge edge, ExecutionContext context) {
        if(edge.getCondition() == null) return true;
        return edgeConditionEvaluator.evaluate(edge, context);
    }


    /**
     * Ensures all required inputs for a node are satisfied, attempting dynamic port adaptation if needed.
     * This method checks for missing required inputs and, if found, attempts to generate compatible
     * port bindings using the port adapter service. Successfully adapted bindings are persisted
     * to the workflow metamodel for future use.
     *
     * @param workflowInstance The workflow instance containing the node and its metamodel
     * @param currentId The ID of the node being prepared for execution
     * @param context The execution context containing available port values
     * @param executionResult the execution result for observability
     * @throws RuntimeException if required inputs cannot be satisfied through port adaptation
     */
    private void ensureRequiredInputsSatisfied(WorkflowInstance workflowInstance, String currentId,
                                               ExecutionContext context, WorkflowObservabilityReport executionResult) {
        NodeInstance node = workflowInstance.getInstanceByWorkflowNodeId(currentId);
        List<String> missingRequiredInputs = getUnsatisfiedInputs(node, context);

        if (missingRequiredInputs.isEmpty()) return;

        logger.info("Node '{}' has missing required inputs: {}. Attempting port adaptation.", currentId, missingRequiredInputs);

        boolean success = attemptPortAdaptationWithTracking(workflowInstance, currentId, context, missingRequiredInputs, executionResult);
        if (!success) throw new RuntimeException("No compatible port adaptations found for node '" + currentId + "'. Missing required inputs: " + missingRequiredInputs);

    }

    /**
     * Attempts to satisfy missing required inputs through dynamic port adaptation.
     * If successful, updates the workflow metamodel with the new bindings.
     * @param workflowInstance The instance of the workflow
     * @param currentId The id of the current node to be executed
     * @param context The execution context
     * @param missingRequiredInputs The list of inputs that the node is missing in order to start its execution
     * @param executionResult the execution result for observability
     * @return Returns true if the adaption was successfully
     */
    private boolean attemptPortAdaptationWithTracking(WorkflowInstance workflowInstance, String currentId,
                                                      ExecutionContext context, List<String> missingRequiredInputs,
                                                      WorkflowObservabilityReport executionResult) {

        NodeInstance node = workflowInstance.getInstanceByWorkflowNodeId(currentId);
        Map<WorkflowEdge, Map<String, String>> newBindingsPerEdge = new HashMap<>();

        List<WorkflowEdge> edges = workflowInstance.getMetamodel().getEdges().stream()
                .filter(edge -> edge.getTargetNodeId().equals(currentId)).toList();

        @SuppressWarnings("unchecked")
        List<Port> targetPorts = (List<Port>) node.getMetamodel().getInputPorts();
        List<Port> sourcePorts = new ArrayList<>();
        Map<String, WorkflowEdge> outputPortsMap = new HashMap<>();

        // Collect source ports
        for (WorkflowEdge edge : edges) {
            NodeInstance sourceNode = workflowInstance.getInstanceByWorkflowNodeId(edge.getSourceNodeId());
            if (sourceNode != null) {
                for (Port outputPort : sourceNode.getMetamodel().getOutputPorts()) {
                    sourcePorts.add(outputPort);
                    if(outputPortsMap.get(outputPort.getKey()) != null) {
                        logger.warn("Output port '{}' is provided by multiple edges. Overriding previous value.", outputPort.getKey());
                    }
                    outputPortsMap.put(outputPort.getKey(), edge);
                }
            }
        }

        // If there are no source ports, we cannot adapt the edges
        if (sourcePorts.isEmpty()) {
            logger.error("No source ports available to adapt edges for node '{}'. Missing required inputs: {}", currentId, missingRequiredInputs);
            executionResult.recordPortAdaptation(currentId, missingRequiredInputs, Collections.emptyMap(), false, new TokenUsage());
            return false;
        }

        // Call the port adapter service to adapt the edges
        var res = portAdapterService.adaptPorts(sourcePorts, targetPorts);

        if (res.getBindings().isEmpty()) {
            logger.error("No compatible edges found to adapt for node '{}'. Missing required inputs: {}", currentId, missingRequiredInputs);
            executionResult.recordPortAdaptation(currentId, missingRequiredInputs, Collections.emptyMap(), false, res.getTokenUsage());
            return false;
        }

        // Loop over each binding that was suggested by the port adapter service
        for (Map.Entry<String, String> binding : res.getBindings().entrySet()) {
            String sourceKey = binding.getKey();
            String targetKey = binding.getValue();

            // Check if the target key is one of the unsatisfied required inputs. If not, skip the binding
            // (we have to handle the case in which the target key is a path, e.g. "TargetPort.contact.email")
            if (missingRequiredInputs.stream().noneMatch(requiredKey ->
                    targetKey.equals(requiredKey) ||
                            targetKey.startsWith(requiredKey + ".") ||
                            requiredKey.startsWith(targetKey + ".")
            )) continue;

            // Apply the binding to the context
            Object sourceValue = context.get(sourceKey);
            if (sourceValue == null) {
                logger.warn("Source key '{}' has no value in context. Skipping binding to target key '{}'", sourceKey, targetKey);
                continue;
            }
            context.put(targetKey, sourceValue);

            // Save the adaptation for persistence later
            WorkflowEdge edge = outputPortsMap.get(sourceKey);
            if (edge != null) {
                newBindingsPerEdge.computeIfAbsent(edge, k -> new HashMap<>()).put(sourceKey, targetKey);
            }
        }

        logger.info("Port adaptation completed for node '{}'. Applied bindings: {}", currentId, res.getBindings());

        // Test if the required inputs are now satisfied
        var unsatisfiedInputs = getUnsatisfiedInputs(node, context);
        boolean adaptationSuccessful = unsatisfiedInputs.isEmpty();


        executionResult.recordPortAdaptation(currentId, missingRequiredInputs, res.getBindings(), adaptationSuccessful, res.getTokenUsage());

        if (!adaptationSuccessful) {
            logger.error("After adaptation, node '{}' still has unsatisfied required inputs: {}", currentId, unsatisfiedInputs);
            return false;
        } else {
            logger.info("All required inputs for node '{}' are now satisfied after adaptation.", currentId);
            persistAdaptedBindings(workflowInstance, currentId, newBindingsPerEdge);
            return true;
        }
    }

    /**
     * Validates that all nodes referenced in edges exist in the node map.
     */
    private void validateEdges(WorkflowInstance workflow, List<WorkflowEdge> edges) {
        for (WorkflowEdge edge : edges) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (workflow.getInstanceByWorkflowNodeId(sourceId) == null)
                logger.warn("Edge references non-existent source node ID: {}", sourceId);
            if (workflow.getInstanceByWorkflowNodeId(targetId) == null)
                logger.warn("Edge references non-existent target node ID: {}", targetId);
        }
    }

    /**
     * Applies the bindings from an edge to copy data in the execution context.
     * Bindings map source port keys to target port keys
     */
    private void applyEdgeBindings(WorkflowInstance workflow, WorkflowEdge edge, ExecutionContext context) {
        for (Map.Entry<String, String> bind : edge.getBindings().entrySet()) {
            String sourceKey = bind.getKey();
            String targetKey = bind.getValue();

            // Attempt to get the source value using dot notation
            Object value = context.get(sourceKey);

            if (value != null) {
                // Source value found, set it at the target using dot notation
                context.put(targetKey, value);
                logger.info("Applied binding: {} -> {} (value: {})", sourceKey, targetKey, value);
            } else {
                // Source key (or path) not found in context, check if the target port has a default value
                String rootTargetKey = targetKey.split("\\.")[0];
                NodeInstance targetNode = workflow.getInstanceByWorkflowNodeId(edge.getTargetNodeId());

                if (targetNode != null) {
                    Port targetPort = findInputPort(targetNode, rootTargetKey);

                    if (targetPort != null && targetPort.getDefaultValue() != null) {
                        context.put(targetKey, targetPort.getDefaultValue());
                        logger.debug("Used default value for target path '{}' (from root port '{}'): {}",
                                targetKey, rootTargetKey, targetPort.getDefaultValue());
                    } else {
                        logger.warn("Cannot apply binding: source key '{}' not found in context and target path '{}' has no default associated with its root port '{}'",
                                sourceKey, targetKey, rootTargetKey);
                    }
                } else {
                    logger.warn("Cannot apply binding: target node not found for edge from {} to {}", edge.getSourceNodeId(), edge.getTargetNodeId());
                }
            }
        }
    }

    /**
     * Prepares inputs for a node by applying default values where needed
     * (only for ports that don't already have values in the context)
     *
     * @param node    the node instance
     * @param context the execution context
     */
    private void prepareNodeInputs(NodeInstance node, ExecutionContext context) {
        for (Port port : node.getMetamodel().getInputPorts()) {
            String portKey = port.getKey();
            // Only apply default if the port doesn't have a value in context
            if (!context.containsKey(portKey) && port.getDefaultValue() != null) {
                context.put(portKey, port.getDefaultValue());
                logger.debug("Applied default value for input port '{}' on node '{}': {}", portKey, node.getId(), port.getDefaultValue());
            }
        }
    }

    /**
     * Helper method to find an input port on a node by key
     *
     * @param node    the node instance
     * @param portKey the key of the port to find
     */
    private Port findInputPort(NodeInstance node, String portKey) {
        for (Port port : node.getMetamodel().getInputPorts()) {
            if (port.getKey().equals(portKey)) return port;
        }
        return null;
    }

    /**
     * Applies default values for output ports that weren't set during node processing.
     * @param node    the node instance that was just processed
     * @param context the execution context
     */
    private void applyDefaultOutputValues(NodeInstance node, ExecutionContext context) {
        for (Port port : node.getMetamodel().getOutputPorts()) {
            String portKey = port.getKey();
            // Only apply default if the port doesn't have a value in context after execution
            if (!context.containsKey(portKey) && port.getDefaultValue() != null) {
                context.put(portKey, port.getDefaultValue());
                logger.debug("Applied default value for output port '{}' on node '{}': {}", portKey, node.getId(), port.getDefaultValue());
            }
        }
    }

    /**
     * Persists adapted port bindings to the workflow metamodel by merging them with existing bindings
     * and ensuring compatibility through validation. Updates all edges in a single batch operation
     * through the MOP service.
     * @param workflowInstance The workflow instance containing the metamodel to update
     * @param currentId The ID of the target node (used for error logging context)
     * @param bindingsPerEdge A map where each key is a WorkflowEdge and each value is a map
     *                        of source-to-target port bindings to be added to that edge
     */
    private void persistAdaptedBindings(WorkflowInstance workflowInstance, String currentId, Map<WorkflowEdge, Map<String, String>> bindingsPerEdge) {
        // Save the adapted bindings for later use
        try {
            logger.info("Saving the adapted bindings for {} edges...", bindingsPerEdge.size());

            // Prepare the batch update map: edgeId -> finalBindings
            Map<String, Map<String, String>> edgeBindingsMap = new HashMap<>();

            for (Map.Entry<WorkflowEdge, Map<String, String>> entry : bindingsPerEdge.entrySet()) {
                WorkflowEdge edge = entry.getKey();
                var source = workflowInstance.getInstanceByWorkflowNodeId(edge.getSourceNodeId());
                var target = workflowInstance.getInstanceByWorkflowNodeId(edge.getTargetNodeId());
                if (source == null || target == null) continue;

                // Merge the new bindings with the existing ones
                Map<String, String> mergedBindings = edge.getBindings() != null ? new HashMap<>(edge.getBindings()) : new HashMap<>();
                mergedBindings.putAll(entry.getValue());

                // Add to batch update map if there are valid bindings
                if (!mergedBindings.isEmpty()) {
                    edgeBindingsMap.put(edge.getId(), mergedBindings);
                }
            }

            // Perform batch update through the MOP service
            if (!edgeBindingsMap.isEmpty()) {
                this.workflowMetamodelService.updateMultipleEdgeBindings(
                        workflowInstance.getMetamodel().getId(),
                        edgeBindingsMap
                );
                logger.info("Successfully persisted adapted bindings for {} edges in workflow {}", edgeBindingsMap.size(), workflowInstance.getMetamodel().getId());
            } else {
                logger.info("No valid bindings to persist for node '{}'", currentId);
            }
        }
        catch (Exception e) {
            logger.error("Error saving adapted bindings for node '{}': {}", currentId, e.getMessage(), e);
        }
    }

    /**
     * Get a list of unsatisfied required inputs for a node instance.
     * @param node the node instance to check
     * @param context the execution context to check against
     * @return a list of keys for unsatisfied required input ports
     */
    private List<String> getUnsatisfiedInputs(NodeInstance node, ExecutionContext context) {
        List<String> unsatisfied = new ArrayList<>();
        for (Port port : node.getMetamodel().getInputPorts()) {
            if (port.getSchema() != null && port.getSchema().getRequired() != null && port.getSchema().getRequired() && !context.containsKey(port.getKey()))
                unsatisfied.add(port.getKey());
        }
        return unsatisfied;
    }



    private static class ExecutionState {
        final Map<String, NodeExecutionState> nodeStates = new HashMap<>();
        final Map<String, List<WorkflowEdge>> outgoingEdges = new HashMap<>();
        final Queue<String> readyQueue = new LinkedList<>();
    }

    private static class NodeExecutionState {
        String nodeId;
        NodeInstance nodeInstance;
        WorkflowNode.ExecutionType executionType;
        int totalIncomingEdges;
        int satisfiedIncomingEdges;
        boolean hasIncomingEdges;
    }
}