package org.caselli.cognitiveworkflow.operational.instances;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.FlowNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Scope("prototype")
public abstract class FlowNodeInstance extends NodeInstance {

    @Override
    public FlowNodeMetamodel getMetamodel() {
        return (FlowNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof FlowNodeMetamodel)) throw new IllegalArgumentException("FlowNodeInstance requires FlowNodeMetamodel");
        super.setMetamodel(metamodel);
    }
}