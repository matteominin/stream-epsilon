package org.caselli.cognitiveworkflow.operational.node;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


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

    public void process(ExecutionContext context) throws Exception {

    }
}