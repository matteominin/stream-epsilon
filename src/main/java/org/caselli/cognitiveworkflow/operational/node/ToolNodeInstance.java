package org.caselli.cognitiveworkflow.operational.node;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.ToolNodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Scope("prototype")
public abstract class ToolNodeInstance extends NodeInstance {

    @Override
    public ToolNodeMetamodel getMetamodel() {
        return (ToolNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof ToolNodeMetamodel)) {
            throw new IllegalArgumentException("ToolNodeInstance requires ToolNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }
}