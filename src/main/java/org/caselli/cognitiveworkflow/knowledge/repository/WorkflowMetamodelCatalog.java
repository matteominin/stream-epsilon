package org.caselli.cognitiveworkflow.knowledge.repository;

import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowMetamodelCatalog extends MongoRepository<WorkflowMetamodel, String> {

    /**
     * Finds workflow metamodels that can handle the specified intent and are enabled.
     * Results sorted by the intent's highest score in descending order
     *
     * @param intentId The ID of the intent to search for
     * @param n Number of workflows to retrieve
     * @return A list of workflow metamodels that handle the intent and are enabled, sorted by score
     */
    @Aggregation(pipeline = {
            "{ \"$match\": { \"handledIntents.intentId\": ?0, \"enabled\": true } }",
            "{ \"$addFields\": { \"matchedIntent\": { \"$filter\": { \"input\": \"$handledIntents\", \"as\": \"intent\", \"cond\": { \"$eq\": [ \"$$intent.intentId\", ?0 ] } } } } }", // Added missing closing brace
            "{ \"$addFields\": { \"matchedScore\": { \"$max\": \"$matchedIntent.score\" } } }",
            "{ \"$sort\": { \"matchedScore\": -1 } }",
            "{ \"$project\": { \"matchedIntent\": 0, \"matchedScore\": 0 } }",
            "{ \"$limit\": ?1 }"
    })
    List<WorkflowMetamodel> findByHandledIntents_IntentId(String intentId, int n);



    /**
     * Finds all workflow metamodels that reference the specified intent
     * This is used when deleting intents to clean up references.
     *
     * @param intentId The ID of the intent to search for
     * @return A list of workflow metamodels that handle the intent
     */
    @Aggregation(pipeline = {
            "{ \"$match\": { \"handledIntents.intentId\": ?0 } }"
    })
    List<WorkflowMetamodel> findAllByHandledIntents_IntentId(String intentId);
}