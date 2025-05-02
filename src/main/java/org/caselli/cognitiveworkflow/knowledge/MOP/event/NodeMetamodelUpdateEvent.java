package org.caselli.cognitiveworkflow.knowledge.MOP.event;

import lombok.Getter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;

@Getter
public class NodeMetamodelUpdateEvent {
    private final String metamodelId;
    private final NodeMetamodel updatedMetamodel;

    public NodeMetamodelUpdateEvent(String metamodelId, NodeMetamodel updatedMetamodel){
        this.metamodelId = metamodelId;
        this.updatedMetamodel = updatedMetamodel;
    }
}
