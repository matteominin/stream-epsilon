package org.caselli.cognitiveworkflow.knowledge.repository;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowMetamodelCatalog extends MongoRepository<WorkflowMetamodel, String> {

    /**
     * Finds workflow metamodels that can handle the specified intent and are enabled.
     * Results sorted by the intent's score in descending order
     *
     * @param intentId The ID of the intent to search for
     * @param pageable Pagination and sorting parameters
     * @return A list of workflow metamodels that handle the intent and are enabled, sorted by score
     */
    @Query(value = "{'handledIntents.intentId': ?0, 'enable': true}", sort = "{'handledIntents.$[element].score': -1}")
    List<WorkflowMetamodel> findByHandledIntents_IntentId(String intentId, org.springframework.data.domain.Pageable pageable);
}
