package org.caselli.cognitiveworkflow.operational.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.operational.LLM.services.IntentDetectionService;
import java.util.List;

/**
 * Intent detection observability trace
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class IntentDetectionObservabilityReport extends ObservabilityReport{

    IntentDetectionService.IntentDetectionResponse.IntentDetectorResult intentDetectorResult;

    String inputRequest;

    List<IntentMetamodel> similarIntents;

    public IntentDetectionObservabilityReport(String inputRequest) {
        this.inputRequest = inputRequest;
    }
}
