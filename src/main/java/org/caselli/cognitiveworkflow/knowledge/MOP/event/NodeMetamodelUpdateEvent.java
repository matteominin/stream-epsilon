package org.caselli.cognitiveworkflow.knowledge.MOP.event;


import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;


public record NodeMetamodelUpdateEvent(String metamodelId, NodeMetamodel updatedMetamodel) {
}
