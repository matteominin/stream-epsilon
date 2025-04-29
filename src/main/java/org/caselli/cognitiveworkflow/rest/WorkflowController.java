package org.caselli.cognitiveworkflow.rest;

import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowMetamodelService workflowService;

    @Autowired
    public WorkflowController(WorkflowMetamodelService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowMetamodel>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }


    @PostMapping
    public ResponseEntity<WorkflowMetamodel> createWorkflow(@RequestBody WorkflowMetamodel workflow) {
        return ResponseEntity.ok(workflowService.createWorkflow(workflow));
    }


}