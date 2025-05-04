package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.node.NodeInstance;
import org.caselli.cognitiveworkflow.operational.workflow.WorkflowInstance;
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
    /** Workflow to execute */
    private final WorkflowInstance workflow;
    /** Map meta-node IDs to instances */
    private final Map<String, NodeInstance> nodeInstancesMap = new HashMap<>();
    /** Map workflow nodes by their workflow-specific id */
    private final  Map<String, WorkflowNode> workflowNodesMap = new HashMap<>();

    public WorkflowExecutor(WorkflowInstance workflow){
        this.workflow = workflow;

        for (NodeInstance node : workflow.getNodeInstances()) nodeInstancesMap.put(node.getId(), node);
        for (WorkflowNode node : workflow.getMetamodel().getNodes()) workflowNodesMap.put(node.getId(), node);
    }

    public void execute(ExecutionContext context) {

        logger.info("-------------------------------------------");

        // Check if the workflow is enabled
        if(!this.workflow.getMetamodel().getEnabled())
            throw new RuntimeException("Cannot execute Workflow " + workflow.getId() + ". It is not enabled.");


        // Validate that all nodes referenced in edges exist
        List<WorkflowEdge> edges = workflow.getMetamodel().getEdges();
        validateEdges(edges);

        // Adjacency list
        Map<String, List<WorkflowEdge>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // outgoings = For each node, store the edges that go out from it
        // inDegree = For each node, store the number of incoming edges
        for (String nodeId : workflowNodesMap.keySet())
            inDegree.put(nodeId, 0);

        for (WorkflowEdge edge : edges) {
            outgoing.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            inDegree.compute(edge.getTargetNodeId(), (k, v) -> v == null ? 1 : v + 1);
        }

        // Starting Queue
        // All the nodes with in-degree 0 (the nodes that can be processed first are those with no incoming edges)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(workflowNodesMap.get(entry.getKey()).getId());
            }
        }


        Set<String> processedNodeIds = new HashSet<>();

        // Process nodes in topological order
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            NodeInstance current = getInstanceByWorkflowNodeId(currentId);

            logger.info("Processing node: {}", currentId);

            if(current == null) {
                logger.error("Node instance not found for ID: {}", currentId);
                continue;
            }

            // Apply default values for any missing inputs
            prepareNodeInputs(current, context);

            try {
                // Process the node
                current.process(context);
                processedNodeIds.add(currentId);
            } catch (Exception e) {
                logger.error("Error processing node {}: {}", currentId, e.getMessage(), e);
                throw new RuntimeException("Error processing node " + currentId, e);
            }

            // Propagate outputs to all the outgoing edges
            List<WorkflowEdge> outs = outgoing.getOrDefault(currentId, Collections.emptyList());

            // Consider all the outgoing edges of the current node
            for (WorkflowEdge edge : outs) {
                String targetId = edge.getTargetNodeId();

                // Get target node
                NodeInstance targetNode = getInstanceByWorkflowNodeId(targetId);

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

            if (getInstanceByWorkflowNodeId(sourceId) == null) logger.warn("Edge references non-existent source node ID: {}", sourceId);
            if (getInstanceByWorkflowNodeId(targetId) == null) logger.warn("Edge references non-existent target node ID: {}", targetId);
        }
    }

    /**
     * Evaluates the condition on an edge to determine if execution should proceed.
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

        if (!pass) logger.debug("Edge condition not met: expected '{}' but got '{}' for port '{}'", expectedValue, actualValue, portKey);

        return pass;
    }

    /**
     * Applies the bindings from an edge to copy data in the execution context.
     * Bindings map source port keys to target port keys.
     */
    private void applyEdgeBindings(WorkflowEdge edge, ExecutionContext context) {
        // Get source and target node instances
        NodeInstance sourceNode = getInstanceByWorkflowNodeId(edge.getSourceNodeId());
        NodeInstance targetNode = getInstanceByWorkflowNodeId(edge.getTargetNodeId());

        if (sourceNode == null || targetNode == null) {
            logger.warn("Cannot apply bindings: source or target node not found");
            return;
        }

        for (Map.Entry<String, String> bind : edge.getBindings().entrySet()) {
            String sourceKey = bind.getKey();
            String targetKey = bind.getValue();

            // First check if the source key is in the context
            if (context.containsKey(sourceKey)) {
                Object value = context.get(sourceKey);
                context.put(targetKey, value);
                logger.debug("Applied binding: {} -> {} (value: {})", sourceKey, targetKey, value);
            } else {
                // Source key not in context, let's check if target port has a default value
                Port targetPort = findInputPort(targetNode, targetKey);
                if (targetPort != null && targetPort.getDefaultValue() != null) {
                    // Apply default value to target
                    context.put(targetKey, targetPort.getDefaultValue());
                    logger.debug("Used default value for target port '{}': {}",
                            targetKey, targetPort.getDefaultValue());
                } else {
                    logger.warn("Cannot apply binding: source key '{}' not found in context and target has no default",
                            sourceKey);
                }
            }
        }
    }

    /***
     * Returns the instance of a node by the workflow-specific node ID
     * @param id the workflow-node ID
     */
    private NodeInstance getInstanceByWorkflowNodeId(String id){
        var workflowNode = this.workflowNodesMap.get(id);
        if(workflowNode == null) return null;
        return this.nodeInstancesMap.get(workflowNode.getNodeMetamodelId());
    }


    /**
     * Prepares inputs for a node by applying default values where needed
     * (only for ports that don't already have values in the context)
     * @param node the node instance
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
     * @param node the node instance
     * @param portKey the key of the port to find
     */
    private Port findInputPort(NodeInstance node, String portKey) {
        for (Port port : node.getMetamodel().getInputPorts()) {
            if (port.getKey().equals(portKey)) {
                return port;
            }
        }
        return null;
    }
}