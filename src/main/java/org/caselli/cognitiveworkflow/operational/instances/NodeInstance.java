package org.caselli.cognitiveworkflow.operational.instances;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.event.EventListener;

@Setter
@Getter
public abstract class NodeInstance {

    private String id;

    // Metamodel
    private NodeMetamodel metamodel;

    public abstract void process(ExecutionContext context) throws Exception;

    @EventListener
    public void onMetaNodeUpdated(NodeMetamodelUpdateEvent event) {
        if (event.getMetamodelId().equals(this.metamodel.getId())) {
            this.metamodel = event.getUpdatedMetamodel();
        }
    }
}