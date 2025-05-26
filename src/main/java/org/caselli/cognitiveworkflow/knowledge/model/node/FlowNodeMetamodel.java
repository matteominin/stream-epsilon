package org.caselli.cognitiveworkflow.knowledge.model.node;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public abstract class FlowNodeMetamodel extends NodeMetamodel {
    @NotNull private ControlType controlType;

    public FlowNodeMetamodel() {
        super();
        this.setType(NodeType.FLOW);
    }

     public enum ControlType {
        GATEWAY
    }
}