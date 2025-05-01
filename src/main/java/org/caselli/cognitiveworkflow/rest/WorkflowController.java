package org.caselli.cognitiveworkflow.rest;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.caselli.cognitiveworkflow.operational.WorkflowInstanceManager;
import org.caselli.cognitiveworkflow.operational.core.WorkflowEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowMetamodelService workflowMetamodelService;
    private final WorkflowInstanceManager workflowInstanceManager;
    private final WorkflowEngine engine;

    @Autowired
    public WorkflowController(WorkflowMetamodelService workflowService,WorkflowInstanceManager workflowInstanceManager, WorkflowEngine engine) {
        this.workflowMetamodelService = workflowService;
        this.workflowInstanceManager = workflowInstanceManager;
        this.engine = engine;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowMetamodel>> getAllWorkflows() {
        return ResponseEntity.ok(workflowMetamodelService.getAllWorkflows());
    }

    @PostMapping
    public ResponseEntity<WorkflowMetamodel> createWorkflow(@RequestBody WorkflowMetamodel workflow) {
        return ResponseEntity.ok(workflowMetamodelService.createWorkflow(workflow));
    }

    @PostMapping("/execute/{workflowId}")
    public ResponseEntity<String> executeWorkflow(@PathVariable String workflowId) {
        // TODO: this implementation body is for test only

        Optional<WorkflowMetamodel> modelRes = this.workflowMetamodelService.getWorkflowById(workflowId);

        if(modelRes.isPresent()){

            // TODO: only for test
            var model = modelRes.get();


            var workflow = this.workflowInstanceManager.getOrCreate(model);

            ExecutionContext context = new ExecutionContext();

            engine.execute(workflow, context);
        }

        return ResponseEntity.ok("ciao");
    }
}