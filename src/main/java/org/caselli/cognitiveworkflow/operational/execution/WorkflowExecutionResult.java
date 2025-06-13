package org.caselli.cognitiveworkflow.operational.execution;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution result for workflow observability
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class WorkflowExecutionResult {

    private final String workflowId;
    private final String workflowName;
    private boolean success;
    private String errorMessage;
    private Throwable exception;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final Instant startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Instant endTime;

    private Duration totalExecutionTime;

    private final Map<String, NodeExecutionDetail> nodeExecutions = new ConcurrentHashMap<>();

    private final List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

    private final List<EdgeEvaluationDetail> edgeEvaluations = Collections.synchronizedList(new ArrayList<>());

    private final List<PortAdaptationDetail> portAdaptations = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Map<String, Object>> contextSnapshots = new ConcurrentHashMap<>();

    private final WorkflowExecutionMetrics metrics = new WorkflowExecutionMetrics();

    public WorkflowExecutionResult(String workflowId, String workflowName) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.startTime = Instant.now();
        this.success = false;
    }

    /**
     * Marks the workflow execution as completed
     */
    public void markCompleted(boolean success, String errorMessage, Throwable exception) {
        this.endTime = Instant.now();
        this.totalExecutionTime = Duration.between(startTime, endTime);
        this.success = success;
        this.errorMessage = errorMessage;
        this.exception = exception;

        calculateMetrics();
    }

    /**
     * Records the start of a node execution
     */
    public void recordNodeStart(String nodeId, String nodeName, String nodeType, ExecutionContext inputContext) {
        NodeExecutionDetail detail = new NodeExecutionDetail(nodeId, nodeName, nodeType);
        detail.recordStart(inputContext);
        nodeExecutions.put(nodeId, detail);
        executionOrder.add(nodeId);

        // Take context snapshot before node execution
        contextSnapshots.put("before_" + nodeId, createContextSnapshot(inputContext));
    }

    /**
     * Records the completion of a node execution
     */
    public void recordNodeCompletion(String nodeId, boolean success, String errorMessage, Throwable exception, ExecutionContext outputContext) {
        NodeExecutionDetail detail = nodeExecutions.get(nodeId);
        if (detail != null) {
            detail.recordCompletion(success, errorMessage, exception, outputContext);

            // Take context snapshot after node execution
            contextSnapshots.put("after_" + nodeId, createContextSnapshot(outputContext));
        }
    }

    /**
     * Records edge evaluation details
     */
    public void recordEdgeEvaluation(String sourceNodeId, String targetNodeId, String edgeId, boolean conditionPassed, String conditionDetails, Map<String, String> appliedBindings) {
        EdgeEvaluationDetail detail = new EdgeEvaluationDetail(
                sourceNodeId, targetNodeId, edgeId, conditionPassed, conditionDetails, appliedBindings, Instant.now()
        );
        edgeEvaluations.add(detail);
    }

    /**
     * Records port adaptation attempts
     */
    public void recordPortAdaptation(String nodeId, List<String> missingInputs, Map<String, String> suggestedBindings, boolean successful) {
        PortAdaptationDetail detail = new PortAdaptationDetail(
                nodeId, missingInputs, suggestedBindings, successful, Instant.now()
        );
        portAdaptations.add(detail);
    }

    /**
     * Creates a safe snapshot of the execution context
     */
    private Map<String, Object> createContextSnapshot(ExecutionContext context) {
        Map<String, Object> snapshot = new HashMap<>();
        for (String key : context.keySet()) {
            Object value = context.get(key);
            snapshot.put(key, value != null ? value.toString() : null);
        }
        return snapshot;
    }

    /**
     * Calculates various metrics
     */
    private void calculateMetrics() {
        metrics.totalNodes = nodeExecutions.size();
        metrics.successfulNodes = (int) nodeExecutions.values().stream()
                .mapToLong(n -> n.success ? 1 : 0).sum();
        metrics.failedNodes = metrics.totalNodes - metrics.successfulNodes;

        // Timing statistics
        List<Duration> executionTimes = nodeExecutions.values().stream()
                .filter(n -> n.executionTime != null)
                .map(n -> n.executionTime)
                .sorted()
                .toList();

        if (!executionTimes.isEmpty()) {
            metrics.fastestNodeTime = executionTimes.get(0);
            metrics.slowestNodeTime = executionTimes.get(executionTimes.size() - 1);

            // Average
            long totalNanos = executionTimes.stream().mapToLong(Duration::toNanos).sum();
            metrics.averageNodeTime = Duration.ofNanos(totalNanos / executionTimes.size());

            // Median
            int medianIndex = executionTimes.size() / 2;
            metrics.medianNodeTime = executionTimes.size() % 2 == 0
                    ? Duration.ofNanos((executionTimes.get(medianIndex - 1).toNanos() +
                    executionTimes.get(medianIndex).toNanos()) / 2)
                    : executionTimes.get(medianIndex);
        }

        metrics.totalEdgeEvaluations = edgeEvaluations.size();
        metrics.passedEdgeEvaluations = (int) edgeEvaluations.stream().mapToLong(e -> e.conditionPassed ? 1 : 0).sum();
        metrics.totalPortAdaptations = portAdaptations.size();
        metrics.successfulPortAdaptations = (int) portAdaptations.stream().mapToLong(p -> p.successful ? 1 : 0).sum();
    }



    public Map<String, NodeExecutionDetail> getNodeExecutions() { return new HashMap<>(nodeExecutions); }
    public List<String> getExecutionOrder() { return new ArrayList<>(executionOrder); }
    public List<EdgeEvaluationDetail> getEdgeEvaluations() { return new ArrayList<>(edgeEvaluations); }
    public List<PortAdaptationDetail> getPortAdaptations() { return new ArrayList<>(portAdaptations); }
    public Map<String, Map<String, Object>> getContextSnapshots() { return new HashMap<>(contextSnapshots); }

    /**
     * Detailed execution information for a single node
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeExecutionDetail {
        // Getters
        private final String nodeId;
        private final String nodeName;
        private final String nodeType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private Instant startTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private Instant endTime;

        private Duration executionTime;
        private boolean success;
        private String errorMessage;
        private Throwable exception;

        // Input/Output tracking
        private Map<String, Object> inputArguments;
        private Map<String, Object> outputResults;

        // Resource usage (if available)
        private Long memoryUsedBytes;
        private final Integer threadId;

        public NodeExecutionDetail(String nodeId, String nodeName, String nodeType) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
            this.threadId = (int) Thread.currentThread().getId();
        }

        public void recordStart(ExecutionContext inputContext) {
            this.startTime = Instant.now();
            this.inputArguments = createArgumentsSnapshot(inputContext);

            // Record memory usage before execution
            Runtime runtime = Runtime.getRuntime();
            this.memoryUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        }

        public void recordCompletion(boolean success, String errorMessage,
                                     Throwable exception, ExecutionContext outputContext) {
            this.endTime = Instant.now();
            this.executionTime = Duration.between(startTime, endTime);
            this.success = success;
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.outputResults = createArgumentsSnapshot(outputContext);
        }

        private Map<String, Object> createArgumentsSnapshot(ExecutionContext context) {
            Map<String, Object> snapshot = new HashMap<>();
            for (String key : context.keySet()) {
                Object value = context.get(key);
                // Create a safe representation for serialization
                if (value != null) {
                    snapshot.put(key, value.toString());
                } else {
                    snapshot.put(key, null);
                }
            }
            return snapshot;
        }

    }

    /**
     * Details about edge condition evaluations
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeEvaluationDetail {
        private final String sourceNodeId;
        private final String targetNodeId;
        private final String edgeId;
        private final boolean conditionPassed;
        private final String conditionDetails;
        private final Map<String, String> appliedBindings;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private final Instant evaluationTime;

        public EdgeEvaluationDetail(String sourceNodeId, String targetNodeId, String edgeId,
                                    boolean conditionPassed, String conditionDetails,
                                    Map<String, String> appliedBindings, Instant evaluationTime) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.edgeId = edgeId;
            this.conditionPassed = conditionPassed;
            this.conditionDetails = conditionDetails;
            this.appliedBindings = appliedBindings != null ? new HashMap<>(appliedBindings) : null;
            this.evaluationTime = evaluationTime;
        }

    }

    /**
     * Details about port adaptation attempts
     */
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PortAdaptationDetail {
        // Getters
        private final String nodeId;
        private final List<String> missingInputs;
        private final Map<String, String> suggestedBindings;
        private final boolean successful;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private final Instant adaptationTime;

        public PortAdaptationDetail(String nodeId, List<String> missingInputs,
                                    Map<String, String> suggestedBindings, boolean successful,
                                    Instant adaptationTime) {
            this.nodeId = nodeId;
            this.missingInputs = missingInputs != null ? new ArrayList<>(missingInputs) : null;
            this.suggestedBindings = suggestedBindings != null ? new HashMap<>(suggestedBindings) : null;
            this.successful = successful;
            this.adaptationTime = adaptationTime;
        }

    }

    /**
     * Aggregated metrics for the workflow execution
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WorkflowExecutionMetrics {
        private int totalNodes;
        private int successfulNodes;
        private int failedNodes;
        private Duration fastestNodeTime;
        private Duration slowestNodeTime;
        private Duration averageNodeTime;
        private Duration medianNodeTime;
        private int totalEdgeEvaluations;
        private int passedEdgeEvaluations;
        private int totalPortAdaptations;
        private int successfulPortAdaptations;
    }
}