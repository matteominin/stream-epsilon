package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Describes the structure and metadata of an Intent that can be processed by a workflow.
 */
@Data
@Document(collection = "intents")
public class IntentMetamodel {
    @Field("_id")
    @Id
    private String id = UUID.randomUUID().toString();

    private String name;
    private String description;


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
