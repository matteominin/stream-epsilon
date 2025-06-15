package org.caselli.cognitiveworkflow.knowledge.model.intent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;


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

    private String description;

    private Boolean AIGenerated;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @JsonIgnore
    private List<Double> embedding;
}
