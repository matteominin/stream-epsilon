package org.caselli.cognitiveworkflow.API.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.WorkflowOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowMetamodelService workflowMetamodelService;
    private final WorkflowOrchestrator workflowOrchestrator;

    @Autowired
    public WorkflowController(WorkflowMetamodelService workflowService,  WorkflowOrchestrator workflowOrchestrator) {
        this.workflowMetamodelService = workflowService;
        this.workflowOrchestrator = workflowOrchestrator;
    }

    @GetMapping
    public ResponseEntity<List<WorkflowMetamodel>> getAllWorkflows() {
        return ResponseEntity.ok(workflowMetamodelService.getAllWorkflows());
    }

    @PostMapping
    public ResponseEntity<WorkflowMetamodel> createWorkflow(@Valid @RequestBody WorkflowMetamodel workflow) throws BadRequestException {
        return ResponseEntity.ok(workflowMetamodelService.createWorkflow(workflow));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowMetamodel> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody WorkflowMetamodel workflow)  {
        return ResponseEntity.ok(workflowMetamodelService.updateWorkflow(id, workflow));
    }

    @PostMapping("/execute")
    public ResponseEntity<Object> execute(@RequestBody ExecuteDTO request) {
        long startTime = System.nanoTime();
        var context = workflowOrchestrator.orchestrateWorkflowExecution(request.request);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Execution time: " + duration + " nanoseconds");


        return ResponseEntity.ok(context);
    }

    @Data
    public static class ExecuteDTO {
        @JsonProperty("request")
        String request;
    }
}