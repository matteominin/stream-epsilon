package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class ToolNodeMetamodel extends NodeMetamodel {

    private ToolType toolType;

    public ToolNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
    }

    public enum ToolType {
        API_REST,
        GRAPHQL,
        SCRIPT_PYTHON,
        JAVA_METHOD,
    }
}