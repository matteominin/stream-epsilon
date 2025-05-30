package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.model.node.*;
import org.caselli.cognitiveworkflow.operational.instances.*;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class NodeFactory {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(NodeFactory.class);
    private final ApplicationContext context;

    public NodeFactory(ApplicationContext context) {
        this.context = context;
    }

    public NodeInstance create(NodeMetamodel metamodel) {

        Class<?> clazz = getNodeInstanceClass(metamodel);
        Object bean = context.getBean(clazz);
        NodeInstance node = (NodeInstance) bean;

        node.setId(metamodel.getId());

        // Set the metamodel
        node.setMetamodel(metamodel);

        this.logger.info("Initializing node instance with ID: " + node.getId() + " and metamodel: " + metamodel.getClass().getName());

        return (NodeInstance) bean;
    }


    private Class<? extends NodeInstance> getNodeInstanceClass(NodeMetamodel metamodel) {
        if (metamodel instanceof LlmNodeMetamodel) {
            return LlmNodeInstance.class;
        } else if (metamodel instanceof RestNodeMetamodel) {
            return RestNodeInstance.class;
        }
        else if (metamodel instanceof EmbeddingsNodeMetamodel) {
            return EmbeddingsNodeInstance.class;
        }
        else if (metamodel instanceof VectorDbNodeMetamodel) {
            return VectorDbNodeInstance.class;
        }
        else if (metamodel instanceof GatewayNodeMetamodel) {
            return GatewayNodeInstance.class;
        }
        else {
            throw new IllegalArgumentException("Unsupported NodeMetamodel type: " + metamodel.getClass().getName());
        }
    }
}
