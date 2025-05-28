package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.annotation.Nonnull;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.WorkflowMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.caselli.cognitiveworkflow.knowledge.validation.ValidationResult;
import org.caselli.cognitiveworkflow.knowledge.validation.WorkflowMetamodelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkflowMetamodelService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMetamodelService.class);

    private final WorkflowMetamodelCatalog repository;
    private final ApplicationEventPublisher eventPublisher;
    final private  WorkflowMetamodelValidator workflowMetamodelValidator;

    public WorkflowMetamodelService(WorkflowMetamodelCatalog repository, ApplicationEventPublisher eventPublisher, WorkflowMetamodelValidator workflowMetamodelValidator) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.workflowMetamodelValidator = workflowMetamodelValidator;
    }

    /**
     * Get all the Workflows metamodel in the MongoDB collection
     * @return All the existing workflows metamodel
     */
    @Cacheable(value = "workflowMetamodels")
    public List<WorkflowMetamodel> getAllWorkflows() {
        return repository.findAll();
    }

    /**
     * Get a specific Workflow metamodel by its ID
     */
    @Cacheable(value = "workflowMetamodels", key = "#id")
    public Optional<WorkflowMetamodel> getWorkflowById(String id) {
        return repository.findById(id);
    }

    /**
     * Save in the DB a new Workflow Metamodel
     * @param workflowMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "workflowMetamodels", allEntries = true)
    public WorkflowMetamodel createWorkflow(WorkflowMetamodel workflowMetamodel) throws BadRequestException {
        if (workflowMetamodel.getId() != null && repository.existsById(workflowMetamodel.getId())) {
            throw new BadRequestException("WorkflowMetamodel with id " + workflowMetamodel.getId() + " already exists.");
        }

        workflowMetamodel.setId(UUID.randomUUID().toString());
        workflowMetamodel.setCreatedAt(LocalDateTime.now());

        // Validate the workflow
        var res = workflowMetamodelValidator.validate(workflowMetamodel);
        if(!res.isValid()) throw new BadRequestException("WorkflowMetamodel is not valid: " + res.getErrors());

        return repository.save(workflowMetamodel);
    }

    /**
     * Update an existing Workflow Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param id Id of the Workflow Metamodel
     * @param updatedData New Workflow Metamodel
     * @return Return the newly saved Document
     */
    @CacheEvict(value = "workflowMetamodels", key = "#id")
    public WorkflowMetamodel updateWorkflow(String id, WorkflowMetamodel updatedData) {

        // Check if the documents exists
        repository.findById(id).orElseThrow(() -> new IllegalArgumentException("WorkflowMetamodel with id " + id + " does not exist."));

        // Validate the workflow
        var res = workflowMetamodelValidator.validate(updatedData);
        if(!res.isValid()) throw new IllegalArgumentException("WorkflowMetamodel is not valid: " + res.getErrors());


        WorkflowMetamodel saved = repository.save(updatedData);

        // Notify the Operational Level of the modification TODO
        eventPublisher.publishEvent(new WorkflowMetamodelUpdateEvent(id, saved));

        return saved;
    }


    /**
     * Update the bindings of a specific edge within a workflow metamodel
     * @param workflowId The ID of the workflow metamodel containing the edge
     * @param edgeId The ID of the edge to update
     * @param newBindings The new bindings to set for the edge
     * @throws IllegalArgumentException if workflow or edge is not found, or if validation fails
     */
    @CacheEvict(value = "workflowMetamodels", key = "#workflowId")
    public void updateEdgeBindings(String workflowId, String edgeId, Map<String, String> newBindings) {

        System.out.println("Updating bindings for edge " + edgeId + " in workflow " + workflowId + " with new bindings: " + newBindings);

        // Retrieve the workflow metamodel
        WorkflowMetamodel workflow = repository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("WorkflowMetamodel with id " + workflowId + " does not exist."));

        // Find the edge to update
        WorkflowEdge targetEdge = workflow.getEdges().stream()
                .filter(edge -> edgeId.equals(edge.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Edge with id " + edgeId + " not found in workflow " + workflowId));

        // Update the bindings
        targetEdge.setBindings(newBindings);

        // Validate the updated workflow
        ValidationResult validationResult = workflowMetamodelValidator.validate(workflow);
        if (!validationResult.isValid()) throw new IllegalArgumentException("Updated workflow is not valid: " + validationResult.getErrors());

        // Save the updated workflow
        WorkflowMetamodel savedWorkflow = repository.save(workflow);

        // Publish update event to notify the Operational Layer TODO
        eventPublisher.publishEvent(new WorkflowMetamodelUpdateEvent(workflowId, savedWorkflow));

        logger.info("Updated bindings for edge {} in workflow {}", edgeId, workflowId);
    }


    /**
     * Delete a workflow metamodel by its ID
     * @param id ID of the workflow metamodel to delete
     */
    @CacheEvict(value = "workflowMetamodels", key = "#id")
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
    @Cacheable(value = "workflowMetamodels", key = "#intentId + '_' + #n")
    public List<WorkflowMetamodel> findTopNHandlingIntent(String intentId, int n) {
        return repository.findByHandledIntents_IntentId(intentId, n);
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        // Only for demo purposes. Not required in production.
        this.validateAllCatalog();
    }

    /**
     * Validates all workflow metamodels in the MongoDB repository.
     * Prints the validation results to the logs.
     */
    public void validateAllCatalog() {
        logger.info("-------------------------------------------------");
        logger.info("Starting validation of Workflow Metamodels on Startup...");

        List<WorkflowMetamodel> workflows = repository.findAll();

        int validCount = 0;
        int invalidCount = 0;
        int totalWarnings = 0;
        int totalErrors = 0;

        for (WorkflowMetamodel workflow : workflows) {

            ValidationResult result = workflowMetamodelValidator.validate(workflow);

            if (result.isValid()) {
                validCount++;
                logger.info("[{}/{}] Workflow metamodel with ID {} is valid.", validCount + invalidCount, workflows.size(), workflow.getId());


                int warningCount = result.getWarningCount();
                if (warningCount > 0) {
                    totalWarnings += warningCount;
                    logger.warn("Found {} warnings for valid workflow ID {}", warningCount, workflow.getId());
                }

                result.printWarnings(logger);

            } else {
                invalidCount++;

                logger.error("[{}/{}] Workflow metamodel with ID {} is invalid", validCount + invalidCount, workflows.size(), workflow.getId());

                // Count and log errors/warnings
                int errorCount = result.getErrorCount();
                int warningCount = result.getWarningCount();
                totalErrors += errorCount;
                totalWarnings += warningCount;

                logger.error("Found {} errors and {} warnings for invalid workflow ID {}", errorCount, warningCount, workflow.getId());

                result.printWarnings(logger);
                result.printErrors(logger);
            }
        }


        logger.info("Validation completed. Results:");
        logger.info(" - Total workflows processed: {}", workflows.size());
        logger.info(" - Valid workflows: {}", validCount);
        logger.info(" - Invalid workflows: {}", invalidCount);
        logger.info(" - Total warnings across all workflows: {}", totalWarnings);
        logger.info(" - Total errors across all workflows: {}", totalErrors);
        logger.info("-------------------------------------------------");
    }
}
