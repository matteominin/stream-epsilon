package org.caselli.cognitiveworkflow.knowledge.MOP;

import lombok.Getter;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;

@Getter
public class NodeMetamodelUpdateEvent {
    private final String metamodelId;
    private final NodeMetamodel updatedMetamodel;

    NodeMetamodelUpdateEvent(String metamodelId,  NodeMetamodel updatedMetamodel){
        this.metamodelId = metamodelId;
        this.updatedMetamodel = updatedMetamodel;
    }
}
