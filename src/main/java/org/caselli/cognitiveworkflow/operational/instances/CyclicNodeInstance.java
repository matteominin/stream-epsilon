package org.caselli.cognitiveworkflow.operational.instances;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
            context.put("iteration", i);
            executeCyclicSubgraph(nodes, edges, context, observabilityReport);
            context.remove("iteration");
        }
    }

    private void executeCyclicSubgraph(List<WorkflowNode> nodes,
            List<WorkflowEdge> edges,
            ExecutionContext context,
            NodeObservabilityReport report) {

        class NodeExecState {
            WorkflowNode node;
            NodeInstance instance;
            int totalIncoming = 0;
            int satisfiedIncoming = 0;
        }

        // Build node states
        Map<String, NodeExecState> stateMap = new HashMap<>();
        for (WorkflowNode node : nodes) {
            NodeExecState state = new NodeExecState();
            state.node = node;
            state.instance = nodeInstanceManager.getOrCreate(node.getNodeMetamodelId());
            stateMap.put(node.getId(), state);
        }

        // Track adjacency and incoming edges
        Map<String, List<String>> adjacency = new HashMap<>();
        for (WorkflowEdge edge : edges) {
            String from = edge.getSourceNodeId();
            String to = edge.getTargetNodeId();

            adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(to);

            NodeExecState targetState = stateMap.get(to);
            if (targetState != null) {
                targetState.totalIncoming++;
            }
        }

        // Queue of nodes with no pending dependencies
        Deque<String> readyQueue = new ArrayDeque<>();
        for (Map.Entry<String, NodeExecState> entry : stateMap.entrySet()) {
            if (entry.getValue().totalIncoming == 0) {
                readyQueue.add(entry.getKey());
            }
        }

        // Process nodes
        while (!readyQueue.isEmpty()) {
            String nodeId = readyQueue.poll();
            NodeExecState nodeState = stateMap.get(nodeId);

            logger.info("Processing node {} in cyclic subgraph", nodeId);
            nodeState.instance.process(context, report);

            List<String> successors = adjacency.getOrDefault(nodeId, List.of());
            for (String succId : successors) {
                NodeExecState succState = stateMap.get(succId);
                if (succState != null) {
                    succState.satisfiedIncoming++;
                    if (succState.satisfiedIncoming >= succState.totalIncoming) {
                        readyQueue.add(succId);
                    }
                }
            }
        }
    }

}
