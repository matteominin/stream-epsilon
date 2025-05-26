package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Class for executing workflows.
 * The WorkflowExecutor orchestrates the execution of a set of WorkflowInstance's nodes,
 * respecting port bindings and transition conditions.
 * Execution progresses in a topological order.
 */
@Component
@Scope("prototype")
public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);
    /**
     * Workflow to execute
     */
    private final WorkflowInstance workflow;

    public WorkflowExecutor(WorkflowInstance workflow) {
        this.workflow = workflow;
    }

    public void execute(ExecutionContext context) {
        execute(context, null);
    }

    public void execute(ExecutionContext context, String startingNodeId) {

        logger.info("-------------------------------------------");

        // Check if the workflow is enabled
        if (!this.workflow.getMetamodel().getEnabled())
            throw new RuntimeException("Cannot execute Workflow " + workflow.getId() + ". It is not enabled.");


        // Validate that all nodes referenced in edges exist
        List<WorkflowEdge> edges = workflow.getMetamodel().getEdges();
        validateEdges(edges);

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

        if (startingNodeId != null) {
            // If startingNodeId is specified, begin from that node
            if (!workflow.getWorkflowNodesMap().containsKey(startingNodeId)) throw new RuntimeException("Starting node with ID " + startingNodeId + " does not exist.");

            queue.add(startingNodeId);

            logger.info("Starting workflow execution from specified node: {}", startingNodeId);
        } else {
            // Otherwise, start from all nodes with in-degree 0
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) queue.add(workflow.getWorkflowNodesMap().get(entry.getKey()).getId());
            }
            logger.info("Starting workflow execution from all entry nodes");
        }


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
                    if (edge.getBindings() != null) applyEdgeBindings(edge, context);

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
    private void validateEdges(List<WorkflowEdge> edges) {
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
    private void applyEdgeBindings(WorkflowEdge edge, ExecutionContext context) {
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
}