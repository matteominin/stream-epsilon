package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class ToolNodeMetamodel extends NodeMetamodel {

    /** Type of the tool */
    private ToolType toolType;

    /** Service endpoint URI */
    private String serviceUri;

    public ToolNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
    }

     public enum ToolType {
        REST
    }
}