package org.caselli.cognitiveworkflow.rest.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.WorkflowInstanceManager;
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
    private final WorkflowInstanceManager workflowInstanceManager;
    private final WorkflowOrchestrator workflowOrchestrator;

    @Autowired
    public WorkflowController(WorkflowMetamodelService workflowService, WorkflowInstanceManager workflowInstanceManager, WorkflowOrchestrator workflowOrchestrator) {
        this.workflowMetamodelService = workflowService;
        this.workflowInstanceManager = workflowInstanceManager;
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

    @PostMapping("/execute")
    public ResponseEntity<String> execute(@RequestBody ExecuteDTO request) {
        workflowOrchestrator.orchestrateWorkflowExecution(request.request);

        // TODO
        return ResponseEntity.ok("ciao");
    }


    @Data
    public static class ExecuteDTO {
        @JsonProperty("request")
        String request;
    }
}