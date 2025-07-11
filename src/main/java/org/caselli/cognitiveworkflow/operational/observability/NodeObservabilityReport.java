package org.caselli.cognitiveworkflow.operational.observability;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Input Mapper observability trace
 * @author niccolocaselli
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class NodeObservabilityReport extends ObservabilityReport {
    String nodeId;
    String workflowId;
    TokenUsage tokenUsage = new TokenUsage(0,0,0);

    public NodeObservabilityReport(String nodeId, String workflowId) {
        this.nodeId = nodeId;
        this.workflowId = workflowId;
    }
}
