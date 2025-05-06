package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@Tag("test")
public class WorkflowMetamodelValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMetamodelValidatorTest.class);

    @Mock
    private NodeMetamodelService nodeMetamodelService;

    private WorkflowMetamodelValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new WorkflowMetamodelValidator(nodeMetamodelService);
    }

    private RestPort createStringPort(String id) {
        return RestPort.resBuilder()
                .withKey(id)
                .withSchema(PortSchema.builder().withRequired(true).stringSchema().build())
                .build();
    }

    private NodeMetamodel createNode(String id, List<RestPort> inputPorts, List<RestPort> outputPorts) {
        RestToolNodeMetamodel nodeMetamodel = new RestToolNodeMetamodel();
        nodeMetamodel.setId(id);
        nodeMetamodel.setName("name");
        nodeMetamodel.setDescription("description");
        nodeMetamodel.setServiceUri("http://localhost:8080/service");
        nodeMetamodel.setInvocationMethod(RestToolNodeMetamodel.InvocationMethod.POST);
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(RestToolNodeMetamodel.ToolType.REST);
        nodeMetamodel.setInputPorts(inputPorts);
        nodeMetamodel.setOutputPorts(outputPorts);
        return nodeMetamodel;
    }

    private WorkflowMetamodel createWorkflow(String id, List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        WorkflowMetamodel workflowMetamodel = new WorkflowMetamodel();
        workflowMetamodel.setId(id);
        workflowMetamodel.setName("name");
        workflowMetamodel.setDescription("description");
        workflowMetamodel.setNodes(nodes);
        workflowMetamodel.setEdges(edges);
        workflowMetamodel.setEnabled(true);
        return workflowMetamodel;
    }


    @Test
    void test(){
        RestPort IA1 = createStringPort("I_A_1");
        RestPort IA2 = createStringPort("I_A_1");
        RestPort OA1 = createStringPort("O_A_1");
        NodeMetamodel nodeA = createNode("node_A", List.of(IA1, IA2), List.of(OA1));
        when(nodeMetamodelService.getNodeById("node_A")).thenReturn(Optional.of(nodeA));

        RestPort IB1 = createStringPort("O_A_1");  // <----
        RestPort OB1 = createStringPort("O_B_1");
        NodeMetamodel nodeB = createNode("node_B", List.of(IB1), List.of(OB1));
        when(nodeMetamodelService.getNodeById("node_B")).thenReturn(Optional.of(nodeB));


        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId(nodeA.getId());

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId(nodeB.getId());

        WorkflowEdge edge = new WorkflowEdge();
        edge.setId("1");
        edge.setSourceNodeId("A");
        edge.setTargetNodeId("B");

        WorkflowMetamodel workflow = createWorkflow("workflow1", List.of(wNodeA, wNodeB),List.of(edge));

        var res = validator.validate(workflow);

        res.printWarnings(logger);
        res.printErrors(logger);

        assertEquals(res.getErrorCount(),0);
        assertEquals(res.getWarningCount(),0);
    }
}
