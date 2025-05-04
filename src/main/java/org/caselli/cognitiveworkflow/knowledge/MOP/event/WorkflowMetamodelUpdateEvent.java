package org.caselli.cognitiveworkflow.knowledge.MOP.event;

import lombok.Getter;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;

@Getter
public class WorkflowMetamodelUpdateEvent {
    private final String metamodelId;
    private final WorkflowMetamodel updatedMetamodel;

    public WorkflowMetamodelUpdateEvent(String metamodelId, WorkflowMetamodel updatedMetamodel){
        this.metamodelId = metamodelId;
        this.updatedMetamodel = updatedMetamodel;
    }
}
