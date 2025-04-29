package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowEdge;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.NodeInstance;
import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Core service for executing workflows.
 * The WorkflowEngine orchestrates the execution of a set of WorkflowInstance nodes,
 * respecting port bindings and transition conditions.
 * Execution progresses in a topological order.
 */
@Service
public class WorkflowEngine {
    private final WorkflowInstance workflow;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    public WorkflowEngine(WorkflowInstance workflow) {
        this.workflow = workflow;
    }

    public void execute(ExecutionContext context) {
        // Map node IDs to instances
        // TODO: il fatto che usiamo come ID dei metamodelli come id delle istanze è un probelma èerché ci limta ad avere solo un metamodello per workflow
        Map<String, NodeInstance> nodeMap = new HashMap<>();
        for (NodeInstance node : workflow.getNodes()) {
            nodeMap.put(node.getId(), node);
        }


        // Retrieve edges from metamodel
        List<WorkflowEdge> edges = workflow.getMetamodel().getEdges();


        // Adjacency list
        Map<String, List<WorkflowEdge>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // outgoings = For each node, store the edges that go out from it
        // inDegree = For each node, store the number of incoming edges

        for (String nodeId : nodeMap.keySet())  inDegree.put(nodeId, 0); // Init inDegree to 0 for all nodes
        for (WorkflowEdge edge : edges) {
            outgoing.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            inDegree.compute(edge.getTargetNodeId(), (k, v) -> v == null ? 1 : v + 1);
        }


        // Starting Queue
        // All the nodes with in-degree 0 (the nodes that can be processed first are those with no incoming edges)
        Queue<NodeInstance> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0) queue.add(nodeMap.get(entry.getKey()));


        // Keep track of the number of processed nodes in order to detect cycles
        int processed = 0;

        // Process nodes in topological order
        while (!queue.isEmpty()) {
            NodeInstance current = queue.poll();
            String currentId = current.getId();

            try {
                logger.info("Processing node: {}", currentId);
                current.process(context);
            } catch (Exception e) {
                logger.error("Error processing node {}: {}", currentId, e.getMessage(), e);
                throw new RuntimeException("Error processing node " + current.getId(), e);
            }

            processed++;

            // Propagate outputs to all the outgoing edges
            List<WorkflowEdge> outs = outgoing.getOrDefault(current.getId(), Collections.emptyList());


            // Consider all the outgoing edges of the current node
            for (WorkflowEdge edge : outs) {

                // Evaluate the edge condition
                WorkflowEdge.Condition cond = edge.getCondition();
                boolean pass = true;
                if (cond != null) {
                    Object val = context.get(cond.getPort());
                    if (val != null) {
                        // Check if the value matches the expected value
                        String expectedValue = cond.getExpectedValue();
                        String actualValue = val.toString();
                        pass = expectedValue.equals(actualValue);
                    } else {
                        pass = false;
                    }
                }

                // Apply any bindings
                if (pass && edge.getBindings() != null) {
                    for (Map.Entry<String, String> bind : edge.getBindings().entrySet()) {
                        String sourceKey = bind.getKey();
                        String targetKey = bind.getValue();
                        if (context.containsKey(sourceKey)) {
                            // Add the new bound value to the context
                            context.put(targetKey, context.get(sourceKey));
                        }else {
                            System.out.println("Context does not contain key: " + sourceKey);
                        }
                    }
                }

                // Decrement in-degree
                String targetId = edge.getTargetNodeId();
                inDegree.compute(targetId, (k, v) -> (v == null ? 0 : v) - 1);

                // Enqueue the target node if it is ready (no incoming edges)
                if (inDegree.get(targetId) == 0) queue.add(nodeMap.get(targetId));
            }
        }

        // Check for cycles
        if (processed != nodeMap.size()) {
            throw new IllegalStateException("Cycle detected in workflow, processed " + processed + " of " + nodeMap.size() + " nodes.");
        }
    }
}
