package org.caselli.cognitiveworkflow.operational;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.WorkflowMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.operational.node.NodeInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

@Setter
@Getter
@Component
@Scope("prototype")
public class WorkflowInstance {
    @NotNull
    public String id;

    // Metamodel
    private WorkflowMetamodel metamodel;

    // Nodes
    private List<NodeInstance> nodeInstances;


    /**
     * Check if the current instance of the workflow can handle an Intent by the intent Id.
     * @param intentId The id of the intent
     * @return Return true if the instance can run the intent
     */
    public boolean canHandleIntent(String intentId) {
        if (metamodel == null || metamodel.getHandledIntents() == null) return false;

        return metamodel.getHandledIntents().stream()
                .anyMatch(intent -> intent.getIntentId().equals(intentId));
    }

    /**
     * Helper method to get the score for a specific intent from the workflow instance
     * @param intentId The ID of the intent
     * @return The score for the intent, or 0.0 if not found
     */
    public Double getScoreForIntent(String intentId) {
        return metamodel.getHandledIntents().stream()
                .filter(intent -> intent.getIntentId().equals(intentId))
                .findFirst()
                .map(WorkflowMetamodel.WorkflowIntentCapability::getScore)
                .orElse(0.0);
    }



    // TODO
    @EventListener
    public void onMetaNodeUpdated(WorkflowMetamodelUpdateEvent event) {
        if (event.getMetamodelId().equals(this.metamodel.getId())) {
            this.metamodel = event.getUpdatedMetamodel();
            // TODO
            // updating the metadata is not sufficient: we have to check what have changed.
            // The DAG structure may have changed
        }
    }
}