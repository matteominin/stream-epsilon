package org.caselli.cognitiveworkflow.knowledge.model.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.UUID;

/**
 * Represents a specific node of a workflow.
 */
@Data
public class WorkflowNode {
    @Id
    @Field("_id")
    private String id = UUID.randomUUID().toString();

    /** The ID of the metamodel of the node*/
    @NotNull
    private String nodeMetamodelId;
}