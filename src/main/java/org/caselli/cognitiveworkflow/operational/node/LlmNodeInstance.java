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

    private LlmNodeMetamodel llmMetamodel;


    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (metamodel instanceof LlmNodeMetamodel) {
            super.setMetamodel(metamodel);
            this.llmMetamodel = (LlmNodeMetamodel) metamodel;
        } else {
            throw new IllegalArgumentException("Cannot assign metamodel of type " + metamodel.getClass().getSimpleName() + " to LlmNodeInstance");
        }
    }


    @Override
    public void process(ExecutionContext context) throws Exception {
        System.out.println("Processing LLM Node Instance: " + getId());

    }
}