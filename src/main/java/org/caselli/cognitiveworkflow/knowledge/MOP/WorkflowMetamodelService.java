package org.caselli.cognitiveworkflow.knowledge.MOP;

import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkflowMetamodelService {

    private final WorkflowMetamodelCatalog repository;

    public WorkflowMetamodelService(WorkflowMetamodelCatalog repository) {
        this.repository = repository;
    }

    public List<WorkflowMetamodel> getAllWorkflows() {
        return repository.findAll();
    }

    public Optional<WorkflowMetamodel> getWorkflowById(String id) {
        return repository.findById(id);
    }

    public WorkflowMetamodel saveWorkflow(WorkflowMetamodel workflow) {
        return repository.save(workflow);
    }

    public void deleteWorkflow(String id) {
        repository.deleteById(id);
    }


    public List<WorkflowMetamodel> findTopNHandlingIntent(String intentId, int n) {
        return repository.findByHandledIntents_IntentId(intentId, PageRequest.of(0, n));
    }
}
