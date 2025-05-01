package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.annotation.Nonnull;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.WorkflowMetamodelCatalog;
import org.caselli.cognitiveworkflow.knowledge.validation.WorkflowMetamodelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        this.validateAllCatalog();
    }

    /**
     * Validates all workflow metamodels in the MongoDB repository.
     * Prints the validation results to the logs.
     */
    public void validateAllCatalog() {
        logger.info("Starting validation of Workflow Metamodels on Startup...");

        List<WorkflowMetamodel> workflows = repository.findAll();

        int validCount = 0;
        int invalidCount = 0;
        int totalWarnings = 0;
        int totalErrors = 0;

        for (WorkflowMetamodel workflow : workflows) {

            WorkflowMetamodelValidator.ValidationResult result = workflowMetamodelValidator.validate(workflow);

            if (result.isValid()) {
                validCount++;
                logger.info("[{}/{}] Workflow metamodel with ID {} is valid.", validCount + invalidCount, workflows.size(), workflow.getId());


                int warningCount = result.getWarningCount();
                if (warningCount > 0) {
                    totalWarnings += warningCount;
                    logger.warn("Found {} warnings for valid workflow ID {}", warningCount, workflow.getId());
                }

                result.printWarnings();

            } else {
                invalidCount++;

                logger.error("[{}/{}] Workflow metamodel with ID {} is invalid", validCount + invalidCount, workflows.size(), workflow.getId());

                // Count and log errors/warnings
                int errorCount = result.getErrorCount();
                int warningCount = result.getWarningCount();
                totalErrors += errorCount;
                totalWarnings += warningCount;

                logger.error("Found {} errors and {} warnings for invalid workflow ID {}", errorCount, warningCount, workflow.getId());

                result.printWarnings();
                result.printErrors();
            }
        }

        logger.info("-------------------------------------------");
        logger.info("Validation completed. Results:");
        logger.info(" - Total workflows processed: {}", workflows.size());
        logger.info(" - Valid workflows: {}", validCount);
        logger.info(" - Invalid workflows: {}", invalidCount);
        logger.info(" - Total warnings across all workflows: {}", totalWarnings);
        logger.info(" - Total errors across all workflows: {}", totalErrors);
    }
}
