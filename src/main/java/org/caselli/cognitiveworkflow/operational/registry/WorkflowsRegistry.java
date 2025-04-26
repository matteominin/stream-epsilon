package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.WorkflowInstance;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkflowsRegistry extends InstancesRegistry<WorkflowInstance> {


    /**
     * Find the top N workflow instances that can handle the specified intent,
     * sorted by their score for the intent (highest first)
     *
     * @param intentId The ID of the intent to handle
     * @param n Number of instances to return
     * @return List of workflow instances that can handle the intent, sorted by score
     */
    public List<WorkflowInstance> findTopNHandlingIntent(String intentId, int n) {
        return runningInstances.values().stream()
                .filter(instance -> instance.canHandleIntent(intentId))
                .sorted((instance1, instance2) -> {
                    // Get the score for this intent from each instance's definition
                    Double score1 = instance1.getScoreForIntent(intentId);
                    Double score2 = instance2.getScoreForIntent(intentId);
                    // Compare in reverse order (highest first)
                    return score2.compareTo(score1);
                })
                .limit(n)
                .collect(Collectors.toList());
    }
}