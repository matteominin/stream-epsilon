package org.caselli.cognitiveworkflow.operational.instances;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.AiNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Scope("prototype")
public abstract class AiNodeInstance extends NodeInstance {

    @Override
    public AiNodeMetamodel getMetamodel() {
        return (AiNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof AiNodeMetamodel)) throw new IllegalArgumentException("AiNodeInstance requires AiNodeMetamodel");
        super.setMetamodel(metamodel);
    }
}