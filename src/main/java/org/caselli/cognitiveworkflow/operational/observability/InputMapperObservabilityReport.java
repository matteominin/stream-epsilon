
package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.AI.services.InputMapperService;
import java.util.List;
import java.util.Map;

/**
 * Input Mapper observability trace
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class InputMapperObservabilityReport extends ObservabilityReport {

    InputMapperService.InputMapperResult inputMapperResult;

    List<IntentMetamodel> similarIntents;

    Map<String, Object> variables;
    List<NodeMetamodel> nodes;
    String requestInput;

    public InputMapperObservabilityReport(Map<String, Object> variables, List<NodeMetamodel> nodes, String requestInput) {
        this.variables = variables;
        this.nodes = nodes;
        this.requestInput = requestInput;
    }
}
