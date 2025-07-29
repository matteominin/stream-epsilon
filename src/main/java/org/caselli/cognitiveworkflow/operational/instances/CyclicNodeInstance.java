package org.caselli.cognitiveworkflow.operational.instances;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.caselli.cognitiveworkflow.knowledge.model.node.CyclicNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.execution.NodeInstanceManager;
import org.caselli.cognitiveworkflow.operational.observability.NodeObservabilityReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CyclicNodeInstance extends FlowNodeInstance {
    @Autowired
    NodeInstanceManager nodeInstanceManager;

    @Override
    public CyclicNodeMetamodel getMetamodel() {
        return (CyclicNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof CyclicNodeMetamodel))
            throw new IllegalArgumentException("CyclicNodeInstance requires CyclicNodeMetamodel");

        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context, NodeObservabilityReport observabilityReport) {
        logger.info("[Node {}]: Processing Cyclic Node Instance", getId());

        CyclicNodeMetamodel metamodel = getMetamodel();
        List<StandardPort> inputPorts = metamodel.getInputPorts();
        List<StandardPort> outputPorts = metamodel.getOutputPorts();
        logger.info("Input Ports: {}", inputPorts);
        logger.info("Output Ports: {}", outputPorts);

        int start = metamodel.getStart();
        int step = metamodel.getStep();
        int end = metamodel.getEnd();
        logger.info("Cyclic parameters - Start: {}, End: {}, Step: {}", start, end, step);

        List<WorkflowNode> nodes = metamodel.getNodes();
        logger.info("Nodes in cycle: {}", nodes);
        List<WorkflowEdge> edges = metamodel.getEdges();
        logger.info("Edges in cycle: {}", edges);

        for (int i = start; i < end; i += step) {
            logger.info("Processing cycle iteration: {}", i);

            context.put("cycleIndex", i);
            executeCyclicSubgraph(nodes, edges, context, observabilityReport);
            context.remove("cycleIndex");
        }
    }

    public void executeCyclicSubgraph(List<WorkflowNode> nodes, List<WorkflowEdge> edges, ExecutionContext context,
            NodeObservabilityReport observabilityReport) {
        logger.info("Executing cyclic subgraph with {} nodes and {} edges", nodes.size(), edges.size());

        Set<String> visitedNodes = new HashSet<>();

        // Find the entry point node
        WorkflowNode entryPoint = nodes.stream()
                .filter(node -> edges.stream().noneMatch(edge -> edge.getTargetNodeId().equals(node.getId())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No entry point found in the graph"));

        // Traverse the graph starting from the entry point
        Deque<WorkflowNode> stack = new ArrayDeque<>();
        stack.push(entryPoint);

        while (!stack.isEmpty()) {
            WorkflowNode currentNode = stack.pop();

            if (visitedNodes.contains(currentNode.getId())) {
                continue;
            }

            // Mark the node as visited
            visitedNodes.add(currentNode.getId());

            // Retrieve or create the NodeInstance
            NodeInstance nodeInstance = nodeInstanceManager.getOrCreate(currentNode.getNodeMetamodelId());

            // Process the node
            nodeInstance.process(context, observabilityReport);

            logger.info("Executed node: {}\n", nodeInstance.getId());

            // Push connected nodes to the stack
            edges.stream()
                    .filter(edge -> edge.getSourceNodeId().equals(currentNode.getId()))
                    .map(edge -> nodes.stream().filter(node -> node.getId().equals(edge.getTargetNodeId())).findFirst()
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .forEach(stack::push);
        }

        logger.info("Cyclic subgraph execution completed.");
    }
}
