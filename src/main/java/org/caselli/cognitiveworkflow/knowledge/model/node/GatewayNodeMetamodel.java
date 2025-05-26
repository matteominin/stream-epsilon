package org.caselli.cognitiveworkflow.knowledge.model.node;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.VectorDbPort;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Collections;
import java.util.List;

/**
 * Represents the metamodel for a Gateway Node, a specialized type of control flow node.
 * This initial implementation provides a Transparent Gateway, which simply
 * forwards data from its input ports directly to its output ports.
 * TODO: Future iterations can introduce specialized Gateway implementations
 * (such as Parallel Gateways, Exclusive Gateways, Inclusive Gateways, Merge and Sync Gateways)
 * to support diverse control flow patterns.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class GatewayNodeMetamodel extends FlowNodeMetamodel {

    public GatewayNodeMetamodel() {
        super();

        this.setControlType(ControlType.GATEWAY);
    }

    /**
        The input ports for this node.
        They are also the output ports as this is a transparent gateway.
     */
    @NotNull private List<VectorDbPort> inputPorts = Collections.emptyList();


    @Override
    @NotNull
    public List<VectorDbPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<VectorDbPort> getOutputPorts() {
        // The output ports mirror the input ports
        return this.inputPorts;
    }

    public void setInputPorts(List<VectorDbPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }
}