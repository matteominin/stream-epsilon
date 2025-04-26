package org.caselli.cognitiveworkflow.operational.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.NodeInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class NodeFactory {
    private final ApplicationContext context;
    private final ObjectMapper mapper;

    public NodeFactory(ApplicationContext context, ObjectMapper mapper) {
        this.context = context;
        this.mapper = mapper;
    }

    public NodeInstance create(NodeMetamodel metamodel) {

        //  TODO: Load the WorkflowNode dynamically based on the class name provided in the metamodel
        //  Class<?> clazz = Class.forName();

        Class<?> clazz = NodeMetamodel.class;
        Object bean = context.getBean(clazz);
        NodeInstance node = (NodeInstance) bean;

        // TODO: inject config
        //mapper.updateValue(node, metamodel.getConfig());

        // TODO: Copy the static fields
        // node.setId(desc.getId());
        // node.setInputKeys(desc.getInputKeys());
        // node.setOutputKeys(desc.getOutputKeys());

        // Set the metamodel
        node.setMetamodel(metamodel);

        return (NodeInstance) bean;
    }
}
