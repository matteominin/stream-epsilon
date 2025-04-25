package org.caselli.cognitiveworkflow.knowledge;

import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowMetamodelService {

    private final WorkflowMetamodelCatalog repository;

    public WorkflowMetamodelService(WorkflowMetamodelCatalog repository) {
        this.repository = repository;
    }

    public List<WorkflowMetamodel> getAllMetaNodes() {
        return repository.findAll();
    }

    public WorkflowMetamodel saveMetaNode(WorkflowMetamodel workflowMetamodel) {
        return repository.save(workflowMetamodel);
    }
}
