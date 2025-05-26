package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
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

    public WorkflowExecutor(WorkflowMetamodelService workflowMetamodelService, PortAdapterService portAdapterService) {
        this.workflowMetamodelService = workflowMetamodelService;
        this.portAdapterService = portAdapterService;
    }

    public void execute(WorkflowInstance workflow, ExecutionContext context) {
        logger.info("-------------------------------------------");

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
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(workflow.getWorkflowNodesMap().get(entry.getKey()).getId());
        }
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



            checkInputSatisfaction(workflow, currentId, context);

            // Check if all required input ports are present
            checkRequiredInputPorts(current, context);

            try {
                logger.info("Current context: {}", context.keySet());

                // Process the node
                current.process(context);
                processedNodeIds.add(currentId);
            } catch (Exception e) {
                logger.error("Error processing node {}: {}", currentId, e.getMessage(), e);
                throw new RuntimeException("Error processing node " + currentId, e);
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
     * Check if the required input ports are present in the context
     */
    private void checkRequiredInputPorts(NodeInstance node, ExecutionContext context) {
        for (Port port : node.getMetamodel().getInputPorts()) {
            if (port.getSchema() != null && port.getSchema().getRequired() != null && port.getSchema().getRequired() && !context.containsKey(port.getKey())) {
                logger.error("Missing required input port '{}' for node '{}'", port.getKey(), node.getId());
                throw new RuntimeException("Missing required input port '" + port.getKey() + "' for node '" + node.getId() + "'");
            }
        }
    }


    private void checkInputSatisfaction( WorkflowInstance workflowInstance, String currentId, ExecutionContext context) {
        NodeInstance node = workflowInstance.getInstanceByWorkflowNodeId(currentId);

        // Get a list of input port marked as required but not present in the context
        List<String> missingRequiredInputs = getUnsatisfiedInputs(node, context);

        // If there are missing required inputs, tries to adapt the edge bindings dynamically
        if (!missingRequiredInputs.isEmpty()) {
            logger.info("Node '{}' has missing required inputs: {}. Attempting to adapt edges.", currentId, missingRequiredInputs);

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
                throw new RuntimeException("No source ports available to adapt edges for node '" + currentId + "'. Missing required inputs: " + missingRequiredInputs);
            }

            logger.info("Starting port adaptation for node '{}'", currentId);

            // Call the port adaptor Service to adapt the edges
            var res = portAdapterService.adaptPorts(sourcePorts, targetPorts);

            if (res.getBindings().isEmpty()) {
                logger.error("No compatible edges found to adapt for node '{}'. Missing required inputs: {}", currentId, missingRequiredInputs);
                throw new RuntimeException("No compatible edges found to adapt for node '" + currentId + "'. Missing required inputs: " + missingRequiredInputs);
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
                throw new RuntimeException("After adaptation, node '" + currentId + "' still has unsatisfied required inputs: " + unsatisfiedInputs);
            } else {
                logger.info("All required inputs for node '{}' are now satisfied after adaptation.", currentId);

                // Save the adapted bindings for later use
                try {
                    logger.info("Saving the adapted bindings...");
                    for (Map.Entry<WorkflowEdge, Map<String, String>> entry : newBindingsPerEdge.entrySet()) {
                        WorkflowEdge edge = entry.getKey();
                        Map<String, String> bindings = entry.getValue();

                        // Merge the new bindings with the existing ones
                        Map<String, String> existingBindings = edge.getBindings() != null ? edge.getBindings() : new HashMap<>();
                        existingBindings.putAll(bindings);

                        // Verify that each binding is valid
                        for (Map.Entry<String, String> binding : existingBindings.entrySet()) {
                            boolean valid = true;

                            String sourceKey = binding.getKey();
                            String targetKey = binding.getValue();

                            // Check that the source key is an output port of the source node
                            NodeInstance sourceNode = workflowInstance.getInstanceByWorkflowNodeId(edge.getSourceNodeId());
                            if (sourceNode == null || sourceNode.getMetamodel() == null || sourceNode.getMetamodel().getOutputPorts() == null)
                                valid = false;
                            else {
                                Port sourcePort = sourceNode.getMetamodel().getOutputPorts().stream()
                                        .filter(port -> port.getKey().equals(sourceKey))
                                        .findFirst()
                                        .orElse(null);

                                if (sourcePort == null) valid = false;

                                // Check that the target key is an input port of the target node
                                NodeInstance targetNode = workflowInstance.getInstanceByWorkflowNodeId(edge.getTargetNodeId());
                                if (targetNode == null || targetNode.getMetamodel() == null || targetNode.getMetamodel().getInputPorts() == null)
                                    valid = false;
                                else {
                                    Port targetPort = targetNode.getMetamodel().getInputPorts().stream()
                                            .filter(port -> port.getKey().equals(targetKey))
                                            .findFirst()
                                            .orElse(null);

                                    if (targetPort == null) valid = false;
                                }
                            }

                            // If the binding is not valid, log a warning and remove it from the existing bindings
                            if(!valid) {
                                logger.warn("Detected invalid binding: {} -> {}. Ignoring this binding.", sourceKey, targetKey);
                                existingBindings.remove(binding.getKey());




                                // TODO: bisogna gestire la dot notation
                            }
                        }

                        // Update the edge bindings in the workflow metamodel
                        this.workflowMetamodelService.updateEdgeBindings(workflowInstance.getMetamodel().getId(), edge.getId(), existingBindings);

                        logger.info("Updated edge from {} to {} with new bindings: {}", edge.getSourceNodeId(), edge.getTargetNodeId(), bindings);
                    }
                }
                catch (Exception e) {
                    logger.error("Error saving adapted bindings for node '{}': {}", currentId, e.getMessage(), e);
                }
            }
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