package org.caselli.cognitiveworkflow.operational;

import org.caselli.cognitiveworkflow.knowledge.NodeLoader;
import org.caselli.cognitiveworkflow.knowledge.NodeMOP;
import org.caselli.cognitiveworkflow.knowledge.WorkflowNodeDescriptor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.List;

@Component
public class NodeBootstrapper {
    private final NodeLoader loader;
    private final NodeMOP mop;

    public NodeBootstrapper(NodeLoader loader, NodeMOP mop) {
        this.loader = loader;
        this.mop = mop;
    }

    @PostConstruct
    public void init() throws Exception {
        List<WorkflowNodeDescriptor> initialDescriptors = loader.loadFromClasspath("workflow.json");
        initialDescriptors.forEach(mop::register);
    }
}