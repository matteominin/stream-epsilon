package org.caselli.cognitiveworkflow.operational.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.caselli.cognitiveworkflow.knowledge.deprecated.WorkflowNodeDescriptor;
import org.caselli.cognitiveworkflow.operational.WorkflowNode;
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

    public WorkflowNode create(WorkflowNodeDescriptor desc) throws Exception {
        // Load the WorkflowNode dynamically based on the class name provided in the descriptor.
        Class<?> clazz = Class.forName(desc.getClassName());
        Object bean = context.getBean(clazz);
        WorkflowNode node = (WorkflowNode) bean;

        // inject config
        mapper.updateValue(node, desc.getConfig());

        // Copy the static fields
        node.setId(desc.getId());
        node.setInputKeys(desc.getInputKeys());
        node.setOutputKeys(desc.getOutputKeys());

        return (WorkflowNode) bean;
    }
}
