package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class RestNodeMetamodel extends ToolNodeMetamodel {

    /** Headers required for service invocation */
    private Map<String, String> headers;

    /** Rest Method */
    @NotNull private InvocationMethod invocationMethod;

    public RestNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
        this.setToolType(ToolNodeMetamodel.ToolType.REST);
    }

    /** Input ports of the node */
    @NotNull private List<RestPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<RestPort> outputPorts = Collections.emptyList();

    @Override
    @NotNull
    public List<RestPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<RestPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<RestPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<RestPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }

    /**
     * REST-specific invocation methods of the service.
     */
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