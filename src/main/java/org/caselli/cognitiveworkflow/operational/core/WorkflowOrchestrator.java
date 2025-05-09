package org.caselli.cognitiveworkflow.operational.core;

import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.InputMapperService;
import org.caselli.cognitiveworkflow.operational.workflow.WorkflowInstance;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowOrchestrator {

    private Logger logger = LoggerFactory.getLogger(WorkflowOrchestrator.class);
    private final ObjectProvider<WorkflowExecutor> executorProvider;
    private final InputMapperService inputMapperService;

    public WorkflowOrchestrator(ObjectProvider<WorkflowExecutor> executorProvider, InputMapperService inputMapperService) {
        this.executorProvider = executorProvider;
        this.inputMapperService = inputMapperService;
    }


    public void startWorkflow(WorkflowInstance workflowInstance, Map<String,Object> variables) {

        // Get the entry points of the workflow
        Set<String> entryPointIDs = workflowInstance.getMetamodel().getEntryNodes();
        List<NodeMetamodel> entryPoints = entryPointIDs.stream()
                .map(id -> workflowInstance.getInstanceByWorkflowNodeId(id).getMetamodel())
                .collect(Collectors.toList());


        var inputMapping = inputMapperService.mapInput(variables, entryPoints);

        if(inputMapping == null){
            throw new RuntimeException("Workflow Failed to start: no starting node can be found.");
        }

        ExecutionContext context = inputMapping.getContext();
        String startingNodeId = inputMapping.getStartingNode().getId();

        WorkflowExecutor executor = executorProvider.getObject(workflowInstance);
        executor.execute(context, startingNodeId);
    }



}
