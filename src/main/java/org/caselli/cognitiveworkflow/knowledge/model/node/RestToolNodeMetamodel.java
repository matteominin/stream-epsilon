package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class RestToolNodeMetamodel extends ToolNodeMetamodel {

    /** Headers required for service invocation */
    private Map<String, String> headers;

    /** Rest Method */
    private InvocationMethod invocationMethod;

    public RestToolNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
        this.setToolType(ToolNodeMetamodel.ToolType.REST);
    }

    public enum InvocationMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS
    }
}