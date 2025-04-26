package org.caselli.cognitiveworkflow.knowledge.MOP;

import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkflowMetamodelService {

    private final WorkflowMetamodelCatalog repository;
    private final ApplicationEventPublisher eventPublisher;

    public WorkflowMetamodelService(WorkflowMetamodelCatalog repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get all the Workflows metamodel in the MongoDB collection
     * @return All the existing workflows metamodel
     */
    public List<WorkflowMetamodel> getAllWorkflows() {
        return repository.findAll();
    }

    /**
     * Get a specific Workflow metamodel by its ID
     */
    public Optional<WorkflowMetamodel> getWorkflowById(String id) {
        return repository.findById(id);
    }

    /**
     * Save in the DB a new Workflow Metamodel
     * @param workflowMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    public WorkflowMetamodel createWorkflow(WorkflowMetamodel workflowMetamodel) {
        if (workflowMetamodel.getId() != null && repository.existsById(workflowMetamodel.getId())) {
            throw new IllegalArgumentException("WorkflowMetamodel with id " + workflowMetamodel.getId() + " already exists.");
        }

        return repository.save(workflowMetamodel);
    }

    /**
     * Update an existing Workflow Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param id Id of the Workflow Metamodel
     * @param updatedData New Workflow Metamodel
     * @return Return the newly saved Document
     */
    public WorkflowMetamodel updateWorkflow(String id, WorkflowMetamodel updatedData) {
        // Check if the documents exists
        WorkflowMetamodel existingNode = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WorkflowMetamodel with id " + id + " does not exist."));

        WorkflowMetamodel saved = repository.save(updatedData);

        // Notify the Operational Level of the modification
        eventPublisher.publishEvent(new WorkflowMetamodelUpdateEvent(id, saved));

        return saved;
    }

    /**
     * Delete a workflow metamodel by its ID
     * @param id ID of the workflow metamodel to delete
     */
    public void deleteWorkflow(String id) {
        repository.deleteById(id);
    }


    /**
     * Finds the first N workflow metamodels that can handle the specified intent.
     * Results sorted by the intent's score in descending order
     * @param intentId The ID of the intent to search for
     * @param n The number of workflows to retrieve
     * @return A list of workflow metamodels that handle the intent, sorted by score
     */
    public List<WorkflowMetamodel> findTopNHandlingIntent(String intentId, int n) {
        return repository.findByHandledIntents_IntentId(intentId, PageRequest.of(0, n));
    }
}
