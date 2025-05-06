package org.caselli.cognitiveworkflow.operational.core;

import lombok.SneakyThrows;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.node.NodeInstance;
import org.caselli.cognitiveworkflow.operational.node.RestToolNodeInstance;
import org.caselli.cognitiveworkflow.operational.workflow.WorkflowInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

@Tag("test")
class WorkflowExecutorTest {

    private WorkflowExecutor executor;
    private WorkflowInstance workflowInstance;
    private RestToolNodeMetamodel nodeMetamodelA;
    private RestToolNodeMetamodel nodeMetamodelB;
    private NodeInstance nodeInstanceA;
    private NodeInstance nodeInstanceB;
    private WorkflowNode wNodeA;
    private WorkflowNode wNodeB;

    @SneakyThrows
    @BeforeEach
    void setUp() {

        // NODE A
        nodeMetamodelA = new RestToolNodeMetamodel();
        nodeMetamodelA.setId("nodeA");
        nodeMetamodelA.setName("Node A");
        nodeMetamodelA.setDescription("Node A description");
        nodeMetamodelA.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodelA.setToolType(RestToolNodeMetamodel.ToolType.REST);
        nodeMetamodelA.setServiceUri("http://example.com/api");

        RestPort inputPortA_1 = RestPort.resBuilder()
                .withKey("inputA_1")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        RestPort inputPortA_2 = RestPort.resBuilder()
                .withKey("inputA_2")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        RestPort outputPortA = RestPort.resBuilder()
                .withKey("outputA")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        nodeMetamodelA.setInputPorts(java.util.List.of((RestPort) inputPortA_1, (RestPort) inputPortA_2));
        nodeMetamodelA.setOutputPorts(java.util.List.of((RestPort) outputPortA));

        nodeInstanceA = mock(RestToolNodeInstance.class);
        nodeInstanceA.setMetamodel(nodeMetamodelA);
        when(nodeInstanceA.getMetamodel()).thenReturn(nodeMetamodelA);
        when(nodeInstanceA.getId()).thenReturn("nodeA");
        // Mock the behavior of Instance A process that add the output to the context
        doAnswer(invocation -> {
            ExecutionContext context = invocation.getArgument(0);
            context.put("outputA", "valueA");
            return null;
        }).when(nodeInstanceA).process(any(ExecutionContext.class));

        // NODE B
        nodeMetamodelB = new RestToolNodeMetamodel();
        nodeMetamodelB.setId("nodeB");
        nodeMetamodelB.setName("Node B");
        nodeMetamodelB.setDescription("Node B description");
        nodeMetamodelB.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodelB.setToolType(RestToolNodeMetamodel.ToolType.REST);

        RestPort inputPortB = RestPort.resBuilder()
                .withKey("inputB")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();

        RestPort outputPortB = RestPort.resBuilder()
                .withKey("outputB")
                .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                .build();


        nodeMetamodelB.setInputPorts(java.util.List.of(inputPortB));
        nodeMetamodelB.setOutputPorts(java.util.List.of(outputPortB));

        nodeInstanceB = mock(RestToolNodeInstance.class);
        nodeInstanceB.setMetamodel(nodeMetamodelB);
        when(nodeInstanceB.getMetamodel()).thenReturn(nodeMetamodelB);
        when(nodeInstanceB.getId()).thenReturn("nodeB");


        // Workflow Metamodel


        WorkflowMetamodel workflowMetamodel = getWorkflowMetamodel();

        // Workflow Instance
        workflowInstance = new WorkflowInstance();
        workflowInstance.setMetamodel(workflowMetamodel);
        workflowInstance.setId("workflow1");
        workflowInstance.setNodeInstances(java.util.List.of(nodeInstanceA, nodeInstanceB));

        // Execution Context
        executor = new WorkflowExecutor(workflowInstance);


    }

    private WorkflowMetamodel getWorkflowMetamodel() {
        wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId(nodeMetamodelA.getId());

        wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId(nodeMetamodelB.getId());


        WorkflowEdge edge = new WorkflowEdge();
        edge.setSourceNodeId(wNodeA.getId());
        edge.setTargetNodeId(wNodeB.getId());

        Map<String,String> bindings = Map.of(
                "outputA", "inputB"
        );
        edge.setBindings(bindings);

        WorkflowMetamodel workflowMetamodel = new WorkflowMetamodel();
        workflowMetamodel.setId("workflow1");
        workflowMetamodel.setName("Workflow 1");
        workflowMetamodel.setDescription("Workflow 1 description");
        workflowMetamodel.setNodes(java.util.List.of(wNodeA, wNodeB));
        workflowMetamodel.setEdges(java.util.List.of(edge));
        workflowMetamodel.setEnabled(true);
        return workflowMetamodel;
    }

    @Test
    void testExecute_directBindings() throws Exception {



        ExecutionContext context = new ExecutionContext();
        context.put("inputA_1", "valueA_1");
        context.put("inputA_2", "valueA_2");

        // Execute the workflow
        executor.execute(context);

        // Verify that the nodes were executed in the correct order
        verify(nodeInstanceA).process(context);
        verify(nodeInstanceB).process(context);

    }
}
