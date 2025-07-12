package org.caselli.cognitiveworkflow.operational.observability;

import lombok.Data;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author niccolocaselli
 */
@Data
public class OrchestrationObservability {
    IntentDetectionObservabilityReport intentDetection;
    RoutingObservabilityReport routing;
    InputMapperObservabilityReport inputMapper;
    WorkflowObservabilityReport workflowExecution;

    TokenUsage tokenUsage;

    /**
     * Calculate the total token usage for the whole process
     */
    public void calcAndSetTotalTokenUsage() {

        List<TokenUsage> tokenUsageList = Stream.concat(
                Stream.of(
                    intentDetection.getTokenUsage(),
                    inputMapper.getTokenUsage()
                ),
                workflowExecution.getTokenUsages().stream()
        ).toList();

        tokenUsage = new TokenUsage(tokenUsageList);

    }


    /**
     * Print a report of the token usage for each step in the orchestration and the total token usage.
     */
    public void printTokenUsageReport(){
        System.out.println("****************** Token Usage Report ******************");

        System.out.println("Intent Detection: " + intentDetection.getTokenUsage().getTotalTokens());
        System.out.println("Input Mapper: " + inputMapper.getTokenUsage().getTotalTokens());
        System.out.println("Workflow Execution: ");
        workflowExecution.getTokenUsages().forEach(
                usage -> System.out.println("  - " + usage.getTotalTokens())
        );
        System.out.println("--------------------------------------------------------");
        System.out.println("Total Prompt Token Usage: " + tokenUsage.getPromptTokens());
        System.out.println("Total Completion Token Usage: " + tokenUsage.getCompletionTokens());
        System.out.println("--------------------------------------------------------");
        System.out.println("Total Token Usage: " + tokenUsage.getTotalTokens());
        System.out.println("********************************************************");

    }
}