package org.caselli.cognitiveworkflow.operational.node;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Scope("prototype")
public class RestToolNodeInstance extends ToolNodeInstance {

    @Override
    public RestToolNodeMetamodel getMetamodel() {
        return (RestToolNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof RestToolNodeMetamodel)) {
            throw new IllegalArgumentException("RestToolNodeInstance requires RestToolNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) throws Exception {
        System.out.println("Processing Rest Tool Node Instance: " + getId());

    }
}