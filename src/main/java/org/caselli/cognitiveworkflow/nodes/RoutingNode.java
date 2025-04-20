package org.caselli.cognitiveworkflow.nodes;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.core.WorkflowNode;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@Getter
@Setter
public class RoutingNode implements WorkflowNode {
    private String id;
    private List<String> inputKeys;
    private List<String> outputKeys;
    private String routingAlgorithm = "dijkstra";

    @Override
    public String getId() { return id; }

    @Override
    public void process(Map<String, Object> context) {
        String intent = (String) context.get(inputKeys.get(0));

        String route = "path-for('" + intent + "') via " + routingAlgorithm;
        context.put(outputKeys.get(0), route);
    }
}
