package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Map;


/**
 * Describes the structure and metadata of an Intent that can be processed by a workflow.
 */
@Data
@Document(collection = "intents")
public class IntentMetamodel {
    @Field("_id")
    @Id
    private String id;
    private String description;
    private Map<String, Object> requiredInputs;
}
