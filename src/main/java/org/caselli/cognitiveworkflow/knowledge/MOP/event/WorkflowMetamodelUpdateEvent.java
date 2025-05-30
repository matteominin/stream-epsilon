package org.caselli.cognitiveworkflow.knowledge.MOP.event;

import lombok.Getter;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;


public record WorkflowMetamodelUpdateEvent(String metamodelId, WorkflowMetamodel updatedMetamodel) {
}
