package org.caselli.cognitiveworkflow.knowledge.model.intent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;


/**
 * Describes the structure and metadata of an Intent that can be processed by a workflow.
 */
@Data
@Document(collection = "intents")
public class IntentMetamodel {
    @Id
    private String id;

    @NotNull
    private String name;

    @NotNull
    private String description;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
