package org.caselli.cognitiveworkflow.operational;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.operational.execution.ObservabilityReport;


/**
 * Observability class for Workflow Routing
 */
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class RoutingObservabilityReport extends ObservabilityReport {

    String intentId;
    String selectedWorkflowId;

    public RoutingObservabilityReport(String intentId){
        this.intentId = intentId;
    }
}
