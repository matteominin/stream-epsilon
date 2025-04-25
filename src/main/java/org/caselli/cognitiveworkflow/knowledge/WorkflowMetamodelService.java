package org.caselli.cognitiveworkflow.knowledge;

import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
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


    public List<WorkflowMetamodel> getAllNodes() {
        return repository.findAll();
    }

    public Optional<WorkflowMetamodel> getNodeById(String id) {
        return repository.findById(id);
    }

}
