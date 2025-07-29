package org.caselli.cognitiveworkflow.operational.instances;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.caselli.cognitiveworkflow.knowledge.model.node.CyclicNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.execution.NodeInstanceManager;
import org.caselli.cognitiveworkflow.operational.observability.NodeObservabilityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Tag("focus")
@ActiveProfiles("test")
public class CyclicNodeInstanceTest {
    @MockitoBean
    private NodeInstanceManager nodeInstanceManager;

    @Mock
    private NodeInstance loopNodeInstance;

    private ExecutionContext context;
    private NodeObservabilityReport report;

    @Autowired
    private ApplicationContext applicationContext; // <- inietta il contesto Spring reale

    @BeforeEach
    public void setup() {
        context = mock(ExecutionContext.class);
        report = mock(NodeObservabilityReport.class);
    }

    @Test
    public void forLoopTest() {
        WorkflowNode node = mock(WorkflowNode.class);
        when(node.getNodeMetamodelId()).thenReturn("dummy-node-id");

        CyclicNodeMetamodel cyclicMetamodel = new CyclicNodeMetamodel();
        cyclicMetamodel.setStart(0);
        cyclicMetamodel.setEnd(3);
        cyclicMetamodel.setStep(1);
        cyclicMetamodel.setNodes(List.of(node));
        cyclicMetamodel.setEdges(List.of(mock(WorkflowEdge.class)));

        NodeInstance dummyNodeInstance = mock(NodeInstance.class);
        when(nodeInstanceManager.getOrCreate("dummy-node-id")).thenReturn(dummyNodeInstance);

        // Prendi l'istanza reale da ApplicationContext
        CyclicNodeInstance cyclicInstance = applicationContext.getBean(CyclicNodeInstance.class);

        cyclicInstance.setMetamodel(cyclicMetamodel);
        cyclicInstance.setId("test-cycle");

        cyclicInstance.process(context, report);

        verify(nodeInstanceManager, times(3)).getOrCreate("dummy-node-id");
        verify(dummyNodeInstance, times(3)).process(context, report);
    }
}