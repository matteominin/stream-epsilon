package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public abstract class ToolNodeMetamodel extends NodeMetamodel {

    /** Type of the tool */
    @NotNull private ToolType toolType;

    /** Service endpoint URI */
    @NotNull private String uri;

    public ToolNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
    }

     public enum ToolType {
        REST,
         VECTOR_DB
    }
}