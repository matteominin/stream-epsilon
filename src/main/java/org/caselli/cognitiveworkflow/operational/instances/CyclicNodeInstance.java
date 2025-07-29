package org.caselli.cognitiveworkflow.operational.instances;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.*;

import org.caselli.cognitiveworkflow.knowledge.model.node.CyclicNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.LoopType;
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
        if (!(metamodel instanceof CyclicNodeMetamodel)) {
            throw new IllegalArgumentException("CyclicNodeInstance requires CyclicNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context, NodeObservabilityReport observabilityReport) {
        logger.info("[Node {}]: Processing Cyclic Node Instance", getId());

        CyclicNodeMetamodel metamodel = getMetamodel();
        List<StandardPort> inputPorts = metamodel.getInputPorts();
        List<StandardPort> outputPorts = metamodel.getOutputPorts();

        // Handle input ports
        handleInputPorts(context, inputPorts);

        if (metamodel.getLoopType() == LoopType.FOREACH) {
            handleForeachLoop(context, metamodel, observabilityReport);
        } else {
            handleForLoop(context, metamodel, observabilityReport);
        }

        logger.info("[Node {}]: Completed processing CyclicNodeInstance", getId());
    }

    private void handleForeachLoop(ExecutionContext context, CyclicNodeMetamodel metamodel,
            NodeObservabilityReport observabilityReport) {
        logger.info("[Node {}]: Executing FOREACH loop", getId());

        String iterableVariableKey = metamodel.getIterableVariable();

        Object iterableVariable = context.get(iterableVariableKey);
        if (!(iterableVariable instanceof Iterable)) {
            logger.error("[Node {}]: FOREACH variable is not iterable", getId());
            throw new IllegalArgumentException("FOREACH variable must be iterable");
        }

        Iterable<?> iterable = (Iterable<?>) iterableVariable;
        for (Object item : iterable) {
            context.put(metamodel.getForeachVariableKey(), item);
            executeCyclicSubgraph(metamodel.getNodes(), metamodel.getEdges(), context, observabilityReport);
            context.remove(metamodel.getForeachVariableKey());
        }
    }

    private void handleForLoop(ExecutionContext context, CyclicNodeMetamodel metamodel,
            NodeObservabilityReport observabilityReport) {
        int start = metamodel.getStart();
        int step = metamodel.getStep();
        int end = metamodel.getEnd();

        logger.info("Cycle parameters - Start: {}, End: {}, Step: {}", start, end, step);

        List<WorkflowNode> nodes = metamodel.getNodes();
        List<WorkflowEdge> edges = metamodel.getEdges();

        for (int i = start; i < end; i += step) {
            logger.info("[Node {}]: Processing cycle iteration {}", getId(), i);
            context.put("cycleIndex", i);

            executeCyclicSubgraph(nodes, edges, context, observabilityReport);

            // Handle output ports
            handleOutputPorts(context, metamodel.getOutputPorts());

            context.remove("cycleIndex");
        }
    }

    private void executeCyclicSubgraph(
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges,
            ExecutionContext context,
            NodeObservabilityReport observabilityReport) {
        logger.info("[Node {}]: Executing cyclic subgraph", getId());

        Set<String> visitedNodes = new HashSet<>();

        WorkflowNode entryPoint = nodes.stream()
                .filter(node -> edges.stream().noneMatch(edge -> edge.getTargetNodeId().equals(node.getId())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No entry point found in the graph"));

        Deque<WorkflowNode> stack = new ArrayDeque<>();
        stack.push(entryPoint);

        while (!stack.isEmpty()) {
            WorkflowNode currentNode = stack.pop();

            if (visitedNodes.contains(currentNode.getId())) {
                continue;
            }

            visitedNodes.add(currentNode.getId());

            NodeInstance nodeInstance = nodeInstanceManager.getOrCreate(currentNode.getNodeMetamodelId());
            nodeInstance.process(context, observabilityReport);

            logger.info("[Node {}]: Executed node: {}", getId(), currentNode.getId());

            edges.stream()
                    .filter(edge -> edge.getSourceNodeId().equals(currentNode.getId()))
                    .forEach(edge -> {
                        applyEdgeBindings(edge, context);

                        nodes.stream()
                                .filter(node -> node.getId().equals(edge.getTargetNodeId()))
                                .findFirst()
                                .ifPresent(stack::push);
                    });
        }

        logger.info("[Node {}]: Finished executing cyclic subgraph", getId());
    }

    /**
     * Applies the bindings from an edge to the execution context.
     * Transfers values from source keys to target keys.
     */
    private void applyEdgeBindings(WorkflowEdge edge, ExecutionContext context) {
        if (edge.getBindings() == null || edge.getBindings().isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> binding : edge.getBindings().entrySet()) {
            String sourceKey = binding.getKey();
            String targetKey = binding.getValue();
            Object value = context.get(sourceKey);

            if (value != null) {
                context.put(targetKey, value);
                logger.debug("[Edge {}]: Binding transferred {} → {}", edge.getId(), sourceKey, targetKey);
            } else {
                logger.debug("[Edge {}]: Source key '{}' not found in context for binding", edge.getId(), sourceKey);
            }
        }
    }

    private void handleOutputPorts(ExecutionContext context, List<StandardPort> outputPorts) {
        if (outputPorts == null || outputPorts.isEmpty()) {
            logger.warn("[Node {}]: No output ports found", getId());
            return;
        }

        for (StandardPort outputPort : outputPorts) {
            Object value = context.get(outputPort.getKey());
            if (value != null) {
                logger.info("[Node {}]: Output port {} has value: {}", getId(), outputPort.getKey(), value);
            } else {
                logger.warn("[Node {}]: Output port {} has no value in context", getId(), outputPort.getKey());
            }
        }
    }

    private void handleInputPorts(ExecutionContext context, List<StandardPort> inputPorts) {
        if (inputPorts == null || inputPorts.isEmpty()) {
            logger.warn("[Node {}]: No input ports found", getId());
            return;
        }

        for (StandardPort inputPort : inputPorts) {
            Object value = context.get(inputPort.getKey());
            if (value != null) {
                logger.info("[Node {}]: Input port {} has value: {}", getId(), inputPort.getKey(), value);
            } else {
                logger.warn("[Node {}]: Input port {} has no value in context", getId(), inputPort.getKey());
            }
        }
    }
}
