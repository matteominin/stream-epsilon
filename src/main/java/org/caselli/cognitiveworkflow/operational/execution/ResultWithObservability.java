package org.caselli.cognitiveworkflow.operational.execution;

/**
 * Helper class to return a result and the observability trace for a process
 * @param <T> The result class
 */
public class ResultWithObservability<T> {
    public ObservabilityReport observabilityReport;

    public T result;

    public ResultWithObservability(T result, ObservabilityReport observabilityReport) {
        this.observabilityReport = observabilityReport;
        this.result = result;
    }
}
