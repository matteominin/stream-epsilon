package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.RestToolNodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@Tag("test")
class WorkflowExecutorTest {

    @Autowired private WorkflowExecutor executor;

    private RestPort createStringPort(String id) {
        return RestPort.builder()
                .withKey(id)
                .withSchema(PortSchema.builder().withRequired(true).stringSchema().build())
                .build();
    }

    private NodeInstance createNodeInstanceA(String id, List<RestPort> inputPorts, List<RestPort> outputPorts) {
        RestToolNodeMetamodel nodeMetamodel = new RestToolNodeMetamodel();
        nodeMetamodel.setId(id);
        nodeMetamodel.setName("name");
        nodeMetamodel.setDescription("description");
        nodeMetamodel.setUri("http://localhost:8080/service");
        nodeMetamodel.setInvocationMethod(RestToolNodeMetamodel.InvocationMethod.POST);
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(RestToolNodeMetamodel.ToolType.REST);
        nodeMetamodel.setInputPorts(inputPorts);
        nodeMetamodel.setOutputPorts(outputPorts);

        RestToolNodeInstance nodeInstance = mock(RestToolNodeInstance.class);
        when(nodeInstance.getMetamodel()).thenReturn(nodeMetamodel);
        when(nodeInstance.getId()).thenReturn(id);

        return nodeInstance;
    }



    private WorkflowInstance createWorkflowInstance(String id, List<NodeInstance> nodeInstances, List<WorkflowNode> nodes,  List<WorkflowEdge> edges) {
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
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1,inputPortA2), List.of(outputPortA1));

        // B
        RestPort inputPortB = createStringPort("outputA_1");
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

        // MOCKING
        doAnswer(invocation -> {
            context.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(context);

        // EXECUTE
        executor.execute(workflowInstance, context);

        // Verify that the nodes were executed in the correct order
        verify(nodeA).process(context);
        verify(nodeB).process(context);
    }

    @Test
    void testExecute_explicitBindings_shouldWork() throws Exception {
        // A
        RestPort inputPortA1 = createStringPort("inputA_1");
        RestPort inputPortA2 = createStringPort("inputA_2");
        RestPort outputPortA1 = createStringPort("outputA_1");
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1,inputPortA2), List.of(outputPortA1));

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

        // MOCKING
        doAnswer(invocation -> {
            context.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(context);

        // EXECUTE
        executor.execute(workflowInstance, context);

        // Verify that the nodes were executed in the correct order
        verify(nodeA).process(context);
        verify(nodeB).process(context);
    }

    @Test
    void testExecute_explicitBindings_shouldFail() throws Exception {
        // A
        RestPort inputPortA1 = createStringPort("inputA_1");
        RestPort inputPortA2 = createStringPort("inputA_2");
        RestPort outputPortA1 = createStringPort("outputA_1");
        NodeInstance nodeA = createNodeInstanceA("nodeA", List.of(inputPortA1,inputPortA2), List.of(outputPortA1));

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

        WorkflowInstance workflowInstance = createWorkflowInstance("workflow1", List.of(nodeA, nodeB), List.of(wNodeA, wNodeB), List.of(edge));

        // INPUT CONTEXT
        ExecutionContext context = new ExecutionContext();
        context.put("inputA_1", "valueA_1");
        context.put("inputA_2", "valueA_2");

        // MOCKING
        doAnswer(invocation -> {
            context.put("outputA_1", "valueA_1");
            return null;
        }).when(nodeA).process(context);

        // EXECUTE
        assertThrows(Exception.class, () -> executor.execute(workflowInstance, context));
    }
}
