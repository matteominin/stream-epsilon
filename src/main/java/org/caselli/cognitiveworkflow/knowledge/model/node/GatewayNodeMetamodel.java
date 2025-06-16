package org.caselli.cognitiveworkflow.knowledge.model.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
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
    @NotNull private List<StandardPort> inputPorts = Collections.emptyList();


    @Override
    @NotNull
    public List<StandardPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @JsonIgnore
    public List<StandardPort> getOutputPorts() {
        // The output ports mirror the input ports
        return this.inputPorts;
    }

    public void setInputPorts(List<StandardPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }
}