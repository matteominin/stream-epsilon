package org.caselli.cognitiveworkflow.knowledge.model.shared;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Map;

/**
 * Represents a specific node of a workflow.
 */
@Data
public class WorkflowNode {
    @Id
    @Field("_id")
    private String id;

    /** The ID of the metamodel of the node*/
    private String nodeMetamodelId;

    /** TODO */
    private Map<String, Object> configurationOverrides;

    /* Specific version of the metamodel */
    private Version version;
}