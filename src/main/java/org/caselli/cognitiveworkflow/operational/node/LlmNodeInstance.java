package org.caselli.cognitiveworkflow.operational.node;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@Scope("prototype")
public class LlmNodeInstance extends NodeInstance {
    @Override
    public LlmNodeMetamodel getMetamodel() {
        return (LlmNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof LlmNodeMetamodel)) {
            throw new IllegalArgumentException("LlmNodeInstance requires LlmNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) throws Exception {
        System.out.println("Processing LLM Node Instance: " + getId());
    }
}