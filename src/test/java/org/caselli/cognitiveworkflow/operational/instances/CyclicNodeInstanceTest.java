package org.caselli.cognitiveworkflow.operational.instances;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.caselli.cognitiveworkflow.knowledge.model.node.CyclicNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.execution.NodeInstanceManager;
import org.caselli.cognitiveworkflow.operational.execution.WorkflowExecutor;
import org.caselli.cognitiveworkflow.operational.observability.NodeObservabilityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public class CyclicNodeInstanceTest {
    @MockitoBean
    private NodeInstanceManager nodeInstanceManager;

    @Mock
    private NodeInstance loopNodeInstance;

    @Autowired
    private WorkflowExecutor executor;

    private ExecutionContext context;
    private NodeObservabilityReport report;

    @Autowired
    private ApplicationContext applicationContext; // <- inietta il contesto Spring reale

    @BeforeEach
    public void setup() {
        context = mock(ExecutionContext.class);
        report = mock(NodeObservabilityReport.class);
    }

    private WorkflowInstance createWorkflowInstance(String id, List<NodeInstance> nodeInstances,
            List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
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

    private NodeInstance createNodeInstance(String id, List<RestPort> inputPorts, List<RestPort> outputPorts) {
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

    @Test
    public void forLoopTest() {
        // Create a real WorkflowNode instance, not a mock
        WorkflowNode node = new WorkflowNode();
        node.setId("workflow-node-id"); // Node ID inside the graph
        node.setNodeMetamodelId("dummy-node-id"); // Metamodel ID used to get NodeInstance

        CyclicNodeMetamodel cyclicMetamodel = new CyclicNodeMetamodel();
        cyclicMetamodel.setStart(0);
        cyclicMetamodel.setEnd(3);
        cyclicMetamodel.setStep(1);
        cyclicMetamodel.setNodes(List.of(node));
        cyclicMetamodel.setEdges(List.of());

        // Mock the NodeInstance and stub getId() to avoid null
        NodeInstance dummyNodeInstance = mock(NodeInstance.class);
        when(dummyNodeInstance.getId()).thenReturn("dummy-instance-id");

        // Stub nodeInstanceManager to return the mocked NodeInstance for the given
        // metamodel ID
        when(nodeInstanceManager.getOrCreate("dummy-node-id")).thenReturn(dummyNodeInstance);

        // Get the CyclicNodeInstance bean from the application context
        CyclicNodeInstance cyclicInstance = applicationContext.getBean(CyclicNodeInstance.class);

        cyclicInstance.setMetamodel(cyclicMetamodel);
        cyclicInstance.setId("test-cycle");

        cyclicInstance.process(context, report);

        // Verify that getOrCreate and process are called 3 times, as expected
        verify(nodeInstanceManager, times(3)).getOrCreate("dummy-node-id");
        verify(dummyNodeInstance, times(3)).process(eq(context), any(NodeObservabilityReport.class));

    }

    @Test
    public void testGraphTraversal() {
        WorkflowNode node1 = mock(WorkflowNode.class);
        WorkflowNode node2 = mock(WorkflowNode.class);
        WorkflowNode node3 = mock(WorkflowNode.class);

        when(node1.getId()).thenReturn("node1");
        when(node2.getId()).thenReturn("node2");
        when(node3.getId()).thenReturn("node3");

        when(node1.getNodeMetamodelId()).thenReturn("metamodel1");
        when(node2.getNodeMetamodelId()).thenReturn("metamodel2");
        when(node3.getNodeMetamodelId()).thenReturn("metamodel3");

        WorkflowEdge edge1 = mock(WorkflowEdge.class);
        WorkflowEdge edge2 = mock(WorkflowEdge.class);

        when(edge1.getSourceNodeId()).thenReturn("node1");
        when(edge1.getTargetNodeId()).thenReturn("node2");
        when(edge2.getSourceNodeId()).thenReturn("node2");
        when(edge2.getTargetNodeId()).thenReturn("node3");

        CyclicNodeMetamodel cyclicMetamodel = new CyclicNodeMetamodel();
        cyclicMetamodel.setStart(0);
        cyclicMetamodel.setEnd(2);
        cyclicMetamodel.setStep(1);
        cyclicMetamodel.setNodes(List.of(node1, node2, node3));
        cyclicMetamodel.setEdges(List.of(edge1, edge2));

        NodeInstance nodeInstance1 = mock(NodeInstance.class);
        NodeInstance nodeInstance2 = mock(NodeInstance.class);
        NodeInstance nodeInstance3 = mock(NodeInstance.class);

        when(nodeInstanceManager.getOrCreate("metamodel1")).thenReturn(nodeInstance1);
        when(nodeInstanceManager.getOrCreate("metamodel2")).thenReturn(nodeInstance2);
        when(nodeInstanceManager.getOrCreate("metamodel3")).thenReturn(nodeInstance3);

        CyclicNodeInstance cyclicInstance = applicationContext.getBean(CyclicNodeInstance.class);

        cyclicInstance.setMetamodel(cyclicMetamodel);
        cyclicInstance.setId("test-cycle");

        cyclicInstance.process(context, report);

        verify(nodeInstanceManager, times(2)).getOrCreate("metamodel1");
        verify(nodeInstanceManager, times(2)).getOrCreate("metamodel2");
        verify(nodeInstanceManager, times(2)).getOrCreate("metamodel3");

        verify(nodeInstance1, times(2)).process(eq(context), any(NodeObservabilityReport.class));
        verify(nodeInstance2, times(2)).process(eq(context), any(NodeObservabilityReport.class));
        verify(nodeInstance3, times(2)).process(eq(context), any(NodeObservabilityReport.class));
    }

    @Test
    @Tag("focus")
    public void testExternalConnections()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        // External node instance
        NodeInstance externalNodeInstance = createNodeInstance("externalNodeId",
                null, null);

        WorkflowNode externalNode = new WorkflowNode();
        externalNode.setId("1");
        externalNode.setNodeMetamodelId("externalNodeId");

        // Loop node instance
        NodeInstance internalNodeInstance = createNodeInstance("internal-node-id", List.of(), List.of());

        WorkflowNode internalNode = new WorkflowNode();
        internalNode.setId("7");
        internalNode.setNodeMetamodelId("internal-node-id");

        // Cyclic node
        CyclicNodeMetamodel cyclicMetamodel = new CyclicNodeMetamodel();
        cyclicMetamodel.setStart(0);
        cyclicMetamodel.setEnd(2);
        cyclicMetamodel.setStep(1);
        cyclicMetamodel.setNodes(List.of(internalNode));
        cyclicMetamodel.setEdges(List.of());
        cyclicMetamodel.setId("cyclic-node-id");
        cyclicMetamodel.setInputPorts(List.of());
        cyclicMetamodel.setOutputPorts(List.of());

        // Inietta automaticamente NodeInstanceManager tramite il contesto Spring
        CyclicNodeInstance realCyclicInstance = applicationContext.getBean(CyclicNodeInstance.class);
        CyclicNodeInstance cyclicInstance = Mockito.spy(realCyclicInstance);
        cyclicInstance.setId("cyclic-node-id");
        cyclicInstance.setMetamodel(cyclicMetamodel);

        // Mock del comportamento di NodeInstanceManager
        when(nodeInstanceManager.getOrCreate("cyclic-node-id")).thenReturn(cyclicInstance);
        when(nodeInstanceManager.getOrCreate("internal-node-id")).thenReturn(internalNodeInstance);

        WorkflowNode cyclicNode = new WorkflowNode();
        cyclicNode.setId("2");
        cyclicNode.setNodeMetamodelId("cyclic-node-id");

        // WorflowEdges
        WorkflowEdge edgeToCyclicNode = new WorkflowEdge();
        edgeToCyclicNode.setId("edge1");
        edgeToCyclicNode.setSourceNodeId("externalNodeId");
        edgeToCyclicNode.setTargetNodeId("cyclic-node-id");

        WorkflowInstance workflow = createWorkflowInstance("test-workflow",
                List.of(cyclicInstance, externalNodeInstance),
                List.of(cyclicNode, externalNode),
                List.of(edgeToCyclicNode));

        executor.execute(workflow, context);

        // Verifica che l'istanza del nodo interno sia stata processata
        InOrder inOrder = inOrder(externalNodeInstance, cyclicInstance, internalNodeInstance);

        inOrder.verify(externalNodeInstance).process(eq(context), any(NodeObservabilityReport.class));
        inOrder.verify(cyclicInstance).process(eq(context), any(NodeObservabilityReport.class));
        inOrder.verify(internalNodeInstance, times(2)).process(eq(context), any(NodeObservabilityReport.class));
    }
}