package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.LLM.services.PortAdapterService;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.RestNodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Tag("test")
class WorkflowExecutorTest {

    @Mock
    private PortAdapterService portAdapterService;

    @Mock
    private WorkflowMetamodelService workflowMetamodelService;

    @Mock
    private WorkflowInstanceManager workflowInstanceManager;

    @Mock
    private NodeInstanceManager nodeInstanceManager;

    @InjectMocks
    private WorkflowExecutor executor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private RestPort createStringPort(String id) {
        return RestPort.builder()
                .withKey(id)
                .withSchema(PortSchema.builder().withRequired(true).stringSchema().build())
                .build();
    }

    private NodeInstance createNodeInstanceA(String id, List<RestPort> inputPorts, List<RestPort> outputPorts) {
        RestNodeMetamodel nodeMetamodel = new RestNodeMetamodel();
        nodeMetamodel.setId(id);
        nodeMetamodel.setName("name");
        nodeMetamodel.setDescription("description");
        nodeMetamodel.setUri("http://localhost:8080/service");
        nodeMetamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.POST);
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(RestNodeMetamodel.ToolType.REST);
        nodeMetamodel.setInputPorts(inputPorts);
        nodeMetamodel.setOutputPorts(outputPorts);

        RestNodeInstance nodeInstance = mock(RestNodeInstance.class);
        when(nodeInstance.getMetamodel()).thenReturn(nodeMetamodel);
        when(nodeInstance.getId()).thenReturn(id);

        return nodeInstance;
    }

    private WorkflowInstance createWorkflowInstance(String id, List<NodeInstance> nodeInstances, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        WorkflowMetamodel workflowMetamodel = new WorkflowMetamodel();
        workflowMetamodel.setId(id);
        workflowMetamodel.setName("name");
        workflowMetamodel.setDescription("description");
        workflowMetamodel.setNodes(nodes);
        workflowMetamodel.setEdges(edges);
        workflowMetamodel.setEnabled(true);

        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setId(id);
        workflowInstance.setNodeInstances(nodeInstances);
        workflowInstance.setMetamodel(workflowMetamodel);
        return workflowInstance;
    }

    @Test
    void testExecute_implicitBindings() throws Exception {
        // A
        RestPort inputPortA1 = createStringPort("inputA_1");
        RestPort inputPortA2 = createStringPort("inputA_2");
        RestPort outputPortA1 = createStringPort("outputA_1");
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1, inputPortA2), List.of(outputPortA1));

        // B
        RestPort inputPortB = createStringPort("outputA_1"); // <-- IMPLICIT BINDING: This matches outputA_1 for implicit binding
        RestPort outputPortB = createStringPort("outputB_1");
        NodeInstance nodeB = createNodeInstanceA("nodeB", List.of(inputPortB), List.of(outputPortB));

        // WORKFLOW
        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId(nodeA.getMetamodel().getId());

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId(nodeB.getMetamodel().getId());

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNodeId("A");
        edge.setTargetNodeId("B");

        WorkflowInstance workflowInstance = createWorkflowInstance("workflow1", List.of(nodeA, nodeB), List.of(wNodeA, wNodeB), List.of(edge));

        // INPUT CONTEXT
        ExecutionContext context = new ExecutionContext();
        context.put("inputA_1", "valueA_1");
        context.put("inputA_2", "valueA_2");

        // MOCKING Node A
        doAnswer(invocation -> {
            ExecutionContext currentContext = invocation.getArgument(0);
            currentContext.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(any(ExecutionContext.class));

        // MOCKING WorkflowMetamodelService behavior
        doNothing().when(workflowMetamodelService).updateMultipleEdgeBindings(anyString(), anyMap());

        // EXECUTE
        executor.execute(workflowInstance, context);

        // Verify that the nodes were executed in the correct order
        verify(nodeA).process(any(ExecutionContext.class));
        verify(nodeB).process(any(ExecutionContext.class));

        // IMPORTANT: Verify that PortAdapterService was NEVER called
        // In implicit binding scenarios, if port names match, the adapter service should not be engaged.
        verify(portAdapterService, never()).adaptPorts(anyList(), anyList());
    }

    @Test
    void testExecute_explicitBindings_shouldWork() throws Exception {
        // A
        RestPort inputPortA1 = createStringPort("inputA_1");
        RestPort inputPortA2 = createStringPort("inputA_2");
        RestPort outputPortA1 = createStringPort("outputA_1");
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1, inputPortA2), List.of(outputPortA1));

        // B
        RestPort inputPortB = createStringPort("inputB");
        RestPort outputPortB = createStringPort("outputB_1");
        NodeInstance nodeB = createNodeInstanceA("nodeB", List.of(inputPortB), List.of(outputPortB));

        // WORKFLOW
        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId(nodeA.getMetamodel().getId());

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId(nodeB.getMetamodel().getId());

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNodeId("A");
        edge.setTargetNodeId("B");
        edge.setBindings(Map.of("outputA_1", "inputB")); // <--------- Explicit binding

        WorkflowInstance workflowInstance = createWorkflowInstance("workflow1", List.of(nodeA, nodeB), List.of(wNodeA, wNodeB), List.of(edge));

        // INPUT CONTEXT
        ExecutionContext context = new ExecutionContext();
        context.put("inputA_1", "valueA_1");
        context.put("inputA_2", "valueA_2");

        // MOCKING Node A's behavior
        doAnswer(invocation -> {
            ExecutionContext currentContext = invocation.getArgument(0);
            currentContext.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(any(ExecutionContext.class));


        // MOCKING WorkflowMetamodelService behavior
        doNothing().when(workflowMetamodelService).updateMultipleEdgeBindings(anyString(), anyMap());

        // EXECUTE
        executor.execute(workflowInstance, context);

        // Verify that the nodes were executed in the correct order
        verify(nodeA).process(any(ExecutionContext.class));
        verify(nodeB).process(any(ExecutionContext.class));


        // IMPORTANT: Verify that PortAdapterService was NEVER called
        // In explicit binding scenarios, if the bindings are provided and valid,
        // the adapter service should not be engaged.
        verify(portAdapterService, never()).adaptPorts(anyList(), anyList());
    }

    @Test
    void testExecute_explicitBindings_shouldFail() throws Exception {
        // A
        RestPort inputPortA1 = createStringPort("inputA_1");
        RestPort inputPortA2 = createStringPort("inputA_2");
        RestPort outputPortA1 = createStringPort("outputA_1");
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1, inputPortA2), List.of(outputPortA1));

        // B
        RestPort inputPortB = createStringPort("inputB"); // This is the required input for Node B
        RestPort outputPortB = createStringPort("outputB_1");
        NodeInstance nodeB = createNodeInstanceA("nodeB", List.of(inputPortB), List.of(outputPortB));

        // WORKFLOW
        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId(nodeA.getMetamodel().getId());

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId(nodeB.getMetamodel().getId());

        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNodeId("A");
        edge.setTargetNodeId("B");
        // No explicit binding here

        WorkflowInstance workflowInstance = createWorkflowInstance("workflow1", List.of(nodeA, nodeB), List.of(wNodeA, wNodeB), List.of(edge));

        // INPUT CONTEXT
        ExecutionContext context = new ExecutionContext();
        context.put("inputA_1", "valueA_1");
        context.put("inputA_2", "valueA_2");

        // MOCKING Node A's behavior
        doAnswer(invocation -> {
            ExecutionContext currentContext = invocation.getArgument(0);
            currentContext.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(any(ExecutionContext.class));

        // MOCKING PortAdapterService to return NO bindings, leading to the expected failure.
        // Since "outputA_1" and "inputB" don't match and there's no explicit binding,
        // we expect adaptPorts to find no solutions.
        var res = new PortAdapterService.PortAdaptation();
        res.setBindings(Map.of());
        when(portAdapterService.adaptPorts(anyList(), anyList()))
                .thenReturn(res);

        // MOCKING WorkflowMetamodelService behavior
        doNothing().when(workflowMetamodelService).updateMultipleEdgeBindings(anyString(), anyMap());

        // EXECUTE and assert that an Exception is thrown
        assertThrows(RuntimeException.class, () -> executor.execute(workflowInstance, context));

        // Verify that nodeA was processed
        verify(nodeA).process(any(ExecutionContext.class));

        // Verify that nodeB's process method was NOT called as the exception should occur before it.
        verify(nodeB, never()).process(any(ExecutionContext.class));

        // IMPORTANT: Verify that PortAdapterService was called
        // In explicit binding scenarios, if the bindings are not provided or invalid,
        // the adapter service should be engaged to find a solution.
        verify(portAdapterService).adaptPorts(anyList(), anyList());
    }
}