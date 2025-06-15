package org.caselli.cognitiveworkflow.operational.instances;

import org.caselli.cognitiveworkflow.knowledge.model.node.GatewayNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
@Component
@Scope("prototype")
public class GatewayNodeInstance extends FlowNodeInstance {
    @Override
    public GatewayNodeMetamodel getMetamodel() {
        return (GatewayNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof GatewayNodeMetamodel)) throw new IllegalArgumentException("GatewayNodeInstance requires GatewayNodeMetamodel");
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) {
        logger.info("[Node {}]: Processing Transparent Gateway Instance", getId());
        // Propagate the content of the input ports to the corresponding (same name) output ports
        // [as for now context is global, we do not need to do anything here]
        // the metamodel will take care of the validation
    }
}