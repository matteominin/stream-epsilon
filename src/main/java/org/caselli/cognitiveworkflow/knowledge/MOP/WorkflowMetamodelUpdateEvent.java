package org.caselli.cognitiveworkflow.knowledge.MOP;

import lombok.Getter;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;

@Getter
public class WorkflowMetamodelUpdateEvent {
    private final String metamodelId;
    private final WorkflowMetamodel updatedMetamodel;

    WorkflowMetamodelUpdateEvent(String metamodelId,  WorkflowMetamodel updatedMetamodel){
        this.metamodelId = metamodelId;
        this.updatedMetamodel = updatedMetamodel;
    }
}
