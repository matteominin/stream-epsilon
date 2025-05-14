package org.caselli.cognitiveworkflow.knowledge.model.node;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public abstract class AiNodeMetamodel extends NodeMetamodel {

    /** Type of the tool */
    @NotNull private ModelType modelType;

    /** Provider of the model */
    @NotNull private String provider;

    /** Name of the model */
    @NotNull private String modelName;

    public AiNodeMetamodel() {
        super();
        this.setType(NodeType.AI);
    }

     public enum ModelType {
        LLM, EMBEDDINGS
    }
}