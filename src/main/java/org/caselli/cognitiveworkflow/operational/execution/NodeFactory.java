package org.caselli.cognitiveworkflow.operational.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.LlmNodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.instances.RestToolNodeInstance;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class NodeFactory {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(NodeFactory.class);
    private final ApplicationContext context;
    private final ObjectMapper mapper;

    public NodeFactory(ApplicationContext context, ObjectMapper mapper) {
        this.context = context;
        this.mapper = mapper;
    }

    public NodeInstance create(NodeMetamodel metamodel) {

        Class<?> clazz = getNodeInstanceClass(metamodel);
        Object bean = context.getBean(clazz);
        NodeInstance node = (NodeInstance) bean;


        node.setId(metamodel.getId());

        // TODO: inject config
        //mapper.updateValue(node, metamodel.getConfig());


        // Set the metamodel
        node.setMetamodel(metamodel);

        this.logger.info("Initializing node instance with ID: " + node.getId() + " and metamodel: " + metamodel.getClass().getName());

        return (NodeInstance) bean;
    }


    private Class<? extends NodeInstance> getNodeInstanceClass(NodeMetamodel metamodel) {
        if (metamodel instanceof LlmNodeMetamodel) {
            return LlmNodeInstance.class;
        } else if (metamodel instanceof RestToolNodeMetamodel) {
            return RestToolNodeInstance.class;
        }
        else {
            throw new IllegalArgumentException("Unsupported NodeMetamodel type: " + metamodel.getClass().getName());
        }
    }
}
