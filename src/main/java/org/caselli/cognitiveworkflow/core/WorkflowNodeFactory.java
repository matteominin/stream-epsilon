package org.caselli.cognitiveworkflow.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class WorkflowNodeFactory {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private ObjectMapper mapper;

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
