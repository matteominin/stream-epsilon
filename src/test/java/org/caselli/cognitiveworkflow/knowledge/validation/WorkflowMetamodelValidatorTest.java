package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return RestPort.builder()
                .withKey(id)
                .withSchema(PortSchema.builder().withRequired(true).stringSchema().build())
                .build();
    }

    private NodeMetamodel createNode(String id, List<RestPort> inputPorts, List<RestPort> outputPorts) {
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
    void validate_shouldPass_whenImplicitBindingMatchesPorts(){
        RestPort IA1 = createStringPort("I_A_1");
        RestPort IA2 = createStringPort("I_A_1");
        RestPort OA1 = createStringPort("O_A_1");
        NodeMetamodel nodeA = createNode("node_A", List.of(IA1, IA2), List.of(OA1));
        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(nodeA));

        RestPort IB1 = createStringPort("O_A_1");  // <---- IMPLICIT BINDING: OA1 => IB1
        RestPort OB1 = createStringPort("O_B_1");
        NodeMetamodel nodeB = createNode("node_B", List.of(IB1), List.of(OB1));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(nodeB));


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

    @Test
    void validate_shouldGenerateWarning_whenImplicitBindingDoesNotMatchPorts(){
        RestPort IA1 = createStringPort("I_A_1");
        RestPort IA2 = createStringPort("I_A_1");
        RestPort OA1 = createStringPort("O_A_1");
        NodeMetamodel nodeA = createNode("node_A", List.of(IA1, IA2), List.of(OA1));
        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(nodeA));

        RestPort IB1 = createStringPort("NO_MATCHING");  // <---- NO MATCHING
        RestPort OB1 = createStringPort("O_B_1");
        NodeMetamodel nodeB = createNode("node_B", List.of(IB1), List.of(OB1));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(nodeB));


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

        assertEquals(res.getErrorCount(),0); // Ports errors should be warning not errors!
        assertTrue(res.getWarningCount() > 0);
    }


    @Test
    void validate_shouldPass_whenExplicitBindingMatchesPorts(){
        RestPort IA1 = createStringPort("I_A_1");
        RestPort IA2 = createStringPort("I_A_1");
        RestPort OA1 = createStringPort("O_A_1");
        NodeMetamodel nodeA = createNode("node_A", List.of(IA1, IA2), List.of(OA1));
        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(nodeA));

        RestPort IB1 = createStringPort("NO_MATCHING");  // <---- NO MATCHING
        RestPort OB1 = createStringPort("O_B_1");
        NodeMetamodel nodeB = createNode("node_B", List.of(IB1), List.of(OB1));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(nodeB));


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
        edge.setBindings(Map.of("O_A_1","NO_MATCHING")); // <--- EXPLICIT BINDING

        WorkflowMetamodel workflow = createWorkflow("workflow1", List.of(wNodeA, wNodeB),List.of(edge));

        var res = validator.validate(workflow);

        res.printWarnings(logger);
        res.printErrors(logger);

        assertEquals(res.getErrorCount(),0);
        assertEquals(res.getWarningCount(),0);
    }

    @Test
    void validate_shouldGenerateErrorOrWarning_whenExplicitBindingDoesNotMatchPorts(){
        RestPort IA1 = createStringPort("I_A_1");
        RestPort IA2 = createStringPort("I_A_1");
        RestPort OA1 = createStringPort("O_A_1");
        NodeMetamodel nodeA = createNode("node_A", List.of(IA1, IA2), List.of(OA1));
        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(nodeA));

        RestPort IB1 = createStringPort("NO_MATCHING");  // <---- NO MATCHING
        RestPort OB1 = createStringPort("O_B_1");
        NodeMetamodel nodeB = createNode("node_B", List.of(IB1), List.of(OB1));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(nodeB));


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
        edge.setBindings(Map.of("O_A_1","NO_MATCHING_2")); // <--- WRONG EXPLICIT BINDING

        WorkflowMetamodel workflow = createWorkflow("workflow1", List.of(wNodeA, wNodeB),List.of(edge));

        var res = validator.validate(workflow);

        res.printWarnings(logger);
        res.printErrors(logger);

        assertTrue(res.getErrorCount() > 0 || res.getWarningCount()>0);
    }

    @Test
    void validate_shouldFail_whenWorkflowHasCycle() {
        // Create nodes
        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId("node_A");

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId("node_B");

        WorkflowNode wNodeC = new WorkflowNode();
        wNodeC.setId("C");
        wNodeC.setNodeMetamodelId("node_C");

        WorkflowNode wNodeD = new WorkflowNode();
        wNodeD.setId("D");
        wNodeD.setNodeMetamodelId("node_D");

        // Create edges forming:
        // A (entry) → B → C → B (cycle) → D
        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setId("1");
        edge1.setSourceNodeId("A");
        edge1.setTargetNodeId("B");

        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setId("2");
        edge2.setSourceNodeId("B");
        edge2.setTargetNodeId("C");

        WorkflowEdge edge3 = new WorkflowEdge();
        edge3.setId("3");
        edge3.setSourceNodeId("C");
        edge3.setTargetNodeId("B");

        WorkflowEdge edge4 = new WorkflowEdge();
        edge4.setId("4");
        edge4.setSourceNodeId("B");
        edge4.setTargetNodeId("D");

        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(createNode("node_A", List.of(), List.of())));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(createNode("node_B", List.of(), List.of())));
        when(nodeMetamodelService.getById("node_C")).thenReturn(Optional.of(createNode("node_C", List.of(), List.of())));
        when(nodeMetamodelService.getById("node_D")).thenReturn(Optional.of(createNode("node_D", List.of(), List.of())));

        WorkflowMetamodel workflow = createWorkflow("cyclic",
                List.of(wNodeA, wNodeB, wNodeC, wNodeD),
                List.of(edge1, edge2, edge3, edge4));

        var res = validator.validate(workflow);

        assertTrue(res.getErrorCount() > 0, "Expected cycle detection error but got: " + res.getErrors());
        assertTrue(res.getErrors().stream()
                        .anyMatch(e -> e.message().contains("Cycle detected") || e.message().contains("Nodes involved in cycles")),
                "Expected cycle detection message but got: " + res.getErrors());
    }


    @Test
    void validate_shouldFail_whenNoEntryPoints() {
        // A -> B -> A
        WorkflowNode wNodeA = new WorkflowNode();
        wNodeA.setId("A");
        wNodeA.setNodeMetamodelId("node_A");

        WorkflowNode wNodeB = new WorkflowNode();
        wNodeB.setId("B");
        wNodeB.setNodeMetamodelId("node_B");

        WorkflowEdge edge1 = new WorkflowEdge();
        edge1.setId("1");
        edge1.setSourceNodeId("A");
        edge1.setTargetNodeId("B");

        WorkflowEdge edge2 = new WorkflowEdge();
        edge2.setId("2");
        edge2.setSourceNodeId("B");
        edge2.setTargetNodeId("A");


        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(createNode("node_A", List.of(), List.of())));
        when(nodeMetamodelService.getById("node_B")).thenReturn(Optional.of(createNode("node_B", List.of(), List.of())));

        WorkflowMetamodel workflow = createWorkflow("cyclic",
                List.of(wNodeA, wNodeB),
                List.of(edge1, edge2));

        var res = validator.validate(workflow);

        assertTrue(res.getErrorCount() > 0);
        assertTrue(res.getErrors().stream()
                        .anyMatch(e -> e.message().contains("entry")));
    }

    @Test
    void validate_shouldFail_whenEdgeReferencesInvalidNodes() {
        WorkflowNode wNode = new WorkflowNode();
        wNode.setId("A");
        wNode.setNodeMetamodelId("node_A");

        WorkflowEdge edge = new WorkflowEdge();
        edge.setId("1");
        edge.setSourceNodeId("A");
        edge.setTargetNodeId("nonexistent_node");

        when(nodeMetamodelService.getById("node_A")).thenReturn(Optional.of(createNode("node_A", List.of(), List.of())));

        WorkflowMetamodel workflow = createWorkflow("invalid_edge", List.of(wNode), List.of(edge));
        var res = validator.validate(workflow);
        assertTrue(res.getErrorCount() > 0);
        assertTrue(res.getErrors().stream().anyMatch(e -> e.message().contains("references non-existent target node")));
    }

    @Test
    void validate_shouldFail_whenWorkflowIsNull() {
        var res = validator.validate(null);
        assertTrue(res.getErrorCount() > 0);
        assertTrue(res.getErrors().stream().anyMatch(e -> e.message().contains("Workflow metamodel cannot be null")));
    }

    @Test
    void validate_shouldFail_whenWorkflowHasNoNodes() {
        WorkflowMetamodel workflow = createWorkflow("empty", Collections.emptyList(), Collections.emptyList());
        var res = validator.validate(workflow);
        assertTrue(res.getErrorCount() > 0);
        assertTrue(res.getErrors().stream().anyMatch(e -> e.message().contains("Workflow must contain at least one node")));
    }

    @Test
    void validate_shouldFail_whenNodeReferencesAreInvalid() {
        WorkflowNode wNode = new WorkflowNode();
        wNode.setId("A");
        wNode.setNodeMetamodelId("nonexistent_node");

        when(nodeMetamodelService.getById("nonexistent_node")).thenReturn(Optional.empty());

        WorkflowMetamodel workflow = createWorkflow("invalid_ref", List.of(wNode), Collections.emptyList());
        var res = validator.validate(workflow);
        assertTrue(res.getErrorCount() > 0);
        assertTrue(res.getErrors().stream().anyMatch(e -> e.message().contains("Referenced node does not exist in repository")));
    }

}
