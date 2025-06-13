package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.operational.LLM.services.PortAdapterService;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Class for executing workflows.
 * The WorkflowExecutor orchestrates the execution of a set of WorkflowInstance's nodes,
 * respecting port bindings and transition conditions.
 * Execution progresses in a topological order.
 */
@Service
public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final WorkflowMetamodelService workflowMetamodelService;

    private final PortAdapterService portAdapterService;

    private final WorkflowInstanceManager workflowInstanceManager;

    private final NodeInstanceManager nodeInstanceManager;


    public WorkflowExecutor(WorkflowMetamodelService workflowMetamodelService, PortAdapterService portAdapterService, WorkflowInstanceManager workflowInstanceManager, NodeInstanceManager nodeInstanceManager) {
        this.workflowMetamodelService = workflowMetamodelService;
        this.portAdapterService = portAdapterService;
        this.workflowInstanceManager = workflowInstanceManager;
        this.nodeInstanceManager = nodeInstanceManager;
    }

    public void execute(WorkflowInstance workflow, ExecutionContext context) {
       try {
           logger.info("-------------------------------------------");

           // Mark the workflow as in execution
           workflowInstanceManager.markRunning(workflow.getId());

           // Check if the workflow is enabled
           if (!workflow.getMetamodel().getEnabled())
               throw new RuntimeException("Cannot execute Workflow " + workflow.getId() + ". It is not enabled.");


           // Validate that all nodes referenced in edges exist
           List<WorkflowEdge> edges = workflow.getMetamodel().getEdges();
           validateEdges(workflow, edges);

           // Adjacency list
           Map<String, List<WorkflowEdge>> outgoing = new HashMap<>();
           Map<String, Integer> inDegree = new HashMap<>();

           // outgoings = For each node, store the edges that go out from it
           // inDegree = For each node, store the number of incoming edges
           for (String nodeId : workflow.getWorkflowNodesMap().keySet())
               inDegree.put(nodeId, 0);

           for (WorkflowEdge edge : edges) {
               outgoing.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
               inDegree.compute(edge.getTargetNodeId(), (k, v) -> v == null ? 1 : v + 1);
           }

           // Starting Queue
           Queue<String> queue = new LinkedList<>();

           // Start from all nodes with in-degree 0
           for (Map.Entry<String, Integer> entry : inDegree.entrySet())
               if (entry.getValue() == 0) queue.add(workflow.getWorkflowNodesMap().get(entry.getKey()).getId());

           logger.info("Starting workflow execution from all entry nodes");


           Set<String> processedNodeIds = new HashSet<>();

           // Process nodes in topological order
           while (!queue.isEmpty()) {
               String currentId = queue.poll();
               NodeInstance current = workflow.getInstanceByWorkflowNodeId(currentId);

               logger.info("*******************************************");
               logger.info("Processing node: {}", currentId);


               if (current == null) {
                   logger.error("Node instance not found for ID: {}", currentId);
                   continue;
               }

               // Apply default values for any missing inputs
               prepareNodeInputs(current, context);

               // Check if all required input ports are present
               // If they are not attempt to fix the workflow edge bindings by invoking the Port Adapter
               ensureRequiredInputsSatisfied(workflow, currentId, context);

               try {
                   logger.info("Current context: {}", context.keySet());

                   // Mark the node as in execution
                   nodeInstanceManager.markRunning(current.getId());

                   // Process the node
                   current.process(context);
                   processedNodeIds.add(currentId);
               } catch (Exception e) {
                   logger.error("Error processing node {}: {}", currentId, e.getMessage(), e);
                   throw new RuntimeException("Error processing node " + currentId, e);
               } finally {
                   // Mark the node as no longer in execution
                   nodeInstanceManager.markFinished(current.getId());
               }

               // Apply default values for any missing outputs
               applyDefaultOutputValues(current, context);

               // Propagate outputs to all the outgoing edges
               List<WorkflowEdge> outs = outgoing.getOrDefault(currentId, Collections.emptyList());

               // Consider all the outgoing edges of the current node
               for (WorkflowEdge edge : outs) {
                   String targetId = edge.getTargetNodeId();

                   // Get target node
                   NodeInstance targetNode = workflow.getInstanceByWorkflowNodeId(targetId);

                   if (targetNode == null) {
                       logger.warn("Edge references non-existent target node ID: {}", targetId);
                       continue;
                   }

                   // Evaluate the edge condition
                   boolean pass = evaluateEdgeCondition(edge, context);

                   if (pass) {
                       // Apply bindings
                       if (edge.getBindings() != null) applyEdgeBindings(workflow, edge, context);

                       // Decrement in-degree if the condition passed
                       inDegree.compute(targetId, (k, v) -> (v == null ? 0 : v) - 1);

                       // Enqueue the target node if it is ready (no incoming edges)
                       if (inDegree.get(targetId) == 0) {
                           queue.add(targetId);
                           logger.info("Node {} is now ready for execution", targetId);
                       }
                   } else {
                       logger.info("Edge condition from {} to {} is not met", currentId, targetId);
                   }
               }
           }

           logger.info("Workflow execution completed successfully. Processed nodes={}", processedNodeIds);
           logger.info("-------------------------------------------");
       } finally {
           // Mark the workflow as no longer in execution
           workflowInstanceManager.markFinished(workflow.getId());
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
     * Evaluates the condition on an edge to determine if execution should proceed.
     *
     * @return true if the condition passes or there is no condition, false otherwise
     */
    private boolean evaluateEdgeCondition(WorkflowEdge edge, ExecutionContext context) {
        WorkflowEdge.Condition cond = edge.getCondition();
        if (cond == null) return true;

        String portKey = cond.getPort();

        Object val = context.get(portKey);

        if (val == null) {
            logger.info("Edge condition failed: port '{}' has null value", portKey);
            return false;
        }

        String expectedValue = cond.getTargetValue();
        String actualValue = val.toString();
        boolean pass = expectedValue.equals(actualValue);

        if (!pass)
            logger.debug("Edge condition not met: expected '{}' but got '{}' for port '{}'", expectedValue, actualValue, portKey);

        return pass;
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
                // This requires finding the *root* port key for the target path.
                String rootTargetKey = targetKey.split("\\.")[0];
                NodeInstance targetNode = workflow.getInstanceByWorkflowNodeId(edge.getTargetNodeId()); // Get target node instance

                if (targetNode != null) {
                    Port targetPort = findInputPort(targetNode, rootTargetKey); // Find the root input port

                    if (targetPort != null && targetPort.getDefaultValue() != null) {
                        // Apply default value to the target using dot notation
                        // Note: This applies the default value of the *root* target port to the *entire target path*.
                        // This might be an area for refinement depending on exact requirements for defaults and dot notation.
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
     * Ensures all required inputs for a node are satisfied, attempting dynamic port adaptation if needed.
     * This method checks for missing required inputs and, if found, attempts to generate compatible
     * port bindings using the port adapter service. Successfully adapted bindings are persisted
     * to the workflow metamodel for future use.
     *
     * @param workflowInstance The workflow instance containing the node and its metamodel
     * @param currentId The ID of the node being prepared for execution
     * @param context The execution context containing available port values
     * @throws RuntimeException if required inputs cannot be satisfied through port adaptation
     */
    private void ensureRequiredInputsSatisfied(WorkflowInstance workflowInstance, String currentId, ExecutionContext context) {
        NodeInstance node = workflowInstance.getInstanceByWorkflowNodeId(currentId);
        List<String> missingRequiredInputs = getUnsatisfiedInputs(node, context);

        if (missingRequiredInputs.isEmpty()) return;

        logger.info("Node '{}' has missing required inputs: {}. Attempting port adaptation.", currentId, missingRequiredInputs);

        boolean success = attemptPortAdaptation(workflowInstance, currentId, context, missingRequiredInputs);
        if (!success) {
            throw new RuntimeException("No compatible port adaptations found for node '" + currentId +
                    "'. Missing required inputs: " + missingRequiredInputs);
        }
    }


    /**
     * Attempts to satisfy missing required inputs through dynamic port adaptation.
     * If successful, updates the workflow metamodel with the new bindings.
     * @param workflowInstance The instance of the workflow
     * @param currentId The id of the current node to be executed
     * @param context The execution context
     * @param missingRequiredInputs The list of inputs that the node is missing in order to start its execution
     * @return Returns true if the adaption was successfully
     */
    private boolean attemptPortAdaptation( WorkflowInstance workflowInstance, String currentId, ExecutionContext context, List<String> missingRequiredInputs) {
        NodeInstance node = workflowInstance.getInstanceByWorkflowNodeId(currentId);

        // Map to store new bindings for each edge (in order to save them later)
        Map<WorkflowEdge, Map<String, String>> newBindingsPerEdge = new HashMap<>();

        // Get the list of edges that have the current node as target
        List<WorkflowEdge> edges = workflowInstance.getMetamodel().getEdges().stream().filter(edge -> edge.getTargetNodeId().equals(currentId)).toList();

        // Target ports (the input ports of the current node)
        @SuppressWarnings("unchecked")
        List<Port> targetPorts = (List<Port>) node.getMetamodel().getInputPorts();

        // Source ports (from all the incoming edges)
        List<Port> sourcePorts = new ArrayList<>();

        // Create a map <portKey, edge> to track the edge that provides each output port to the current node
        Map<String, WorkflowEdge> outputPortsMap = new HashMap<>();

        for (WorkflowEdge edge : edges) {
            NodeInstance sourceNode = workflowInstance.getInstanceByWorkflowNodeId(edge.getSourceNodeId());
            if (sourceNode != null) {
                for (Port outputPort : sourceNode.getMetamodel().getOutputPorts()) {
                    // Add all the output ports of the source node to the sourcePorts list
                    sourcePorts.add(outputPort);

                    // Track which node provides each output port
                    if(outputPortsMap.get(outputPort.getKey()) != null) logger.warn("Output port '{}' is provided by multiple edges. Overriding previous value.", outputPort.getKey());
                    outputPortsMap.put(outputPort.getKey(), edge);
                }
            }
        }

        // If there are no source ports, we cannot adapt the edges
        if (sourcePorts.isEmpty()) {
            logger.error("No source ports available to adapt edges for node '{}'. Missing required inputs: {}", currentId, missingRequiredInputs);
            return false;
        }

        // Call the port adaptor Service to adapt the edges
        var res = portAdapterService.adaptPorts(sourcePorts, targetPorts);

        if (res.getBindings().isEmpty()) {
            logger.error("No compatible edges found to adapt for node '{}'. Missing required inputs: {}", currentId, missingRequiredInputs);
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

            // Save the adaptation to save later
            WorkflowEdge edge = outputPortsMap.get(sourceKey);
            if (edge != null) newBindingsPerEdge.computeIfAbsent(edge, k -> new HashMap<>()).put(sourceKey, targetKey);
        }

        logger.info("Port adaptation completed for node '{}'. Applied bindings: {}", currentId, res.getBindings());

        // Test if the required inputs are now satisfied
        var unsatisfiedInputs = getUnsatisfiedInputs(node, context);
        if (!unsatisfiedInputs.isEmpty()) {
            logger.error("After adaptation, node '{}' still has unsatisfied required inputs: {}", currentId, unsatisfiedInputs);
            return false;
        } else {
            logger.info("All required inputs for node '{}' are now satisfied after adaptation.", currentId);
            persistAdaptedBindings(workflowInstance, currentId, newBindingsPerEdge);
            return true;
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
                if (!mergedBindings.isEmpty()) edgeBindingsMap.put(edge.getId(), mergedBindings);
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
            if (port.getSchema() != null && port.getSchema().getRequired() != null && port.getSchema().getRequired() && !context.containsKey(port.getKey())) {
                unsatisfied.add(port.getKey());
            }
        }
        return unsatisfied;
    }
}