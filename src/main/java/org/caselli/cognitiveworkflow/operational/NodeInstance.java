package org.caselli.cognitiveworkflow.operational;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Setter
@Getter
@Component
@Scope("prototype")
public class NodeInstance {

    public String id;

    // Metamodel
    private NodeMetamodel metamodel;

    @EventListener
    public void onMetaNodeUpdated(NodeMetamodelUpdateEvent event) {
        if (event.getMetamodelId().equals(this.metamodel.getId())) {
            this.metamodel = event.getUpdatedMetamodel();
        }
    }

    void process(Map<String, Object> context) throws Exception {}
}