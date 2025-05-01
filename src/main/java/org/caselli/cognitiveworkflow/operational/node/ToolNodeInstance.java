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
public class ToolNodeInstance extends NodeInstance {

    private ToolNodeMetamodel toolMetamodel;

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (metamodel instanceof ToolNodeMetamodel) {
            super.setMetamodel(metamodel);
            this.toolMetamodel = (ToolNodeMetamodel) metamodel;
        } else {
            throw new IllegalArgumentException("Cannot assign metamodel of type " + metamodel.getClass().getSimpleName() + " to ToolNodeInstance");
        }
    }

    @Override
    public void process(ExecutionContext context) throws Exception {
        System.out.println("Processing Tool Node Instance: " + getId());

    }
}