package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Port;
import org.caselli.cognitiveworkflow.knowledge.model.shared.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.shared.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.shared.WorkflowEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating WorkflowMetamodels correctness.
 * - DAG structure verification
 * - Node reference validity
 * - Port compatibility between connected nodes
 */
@Service
public class WorkflowMetamodelValidator {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowMetamodelValidator.class);


    /**
     * Helper class for storing validation results
     */
    public static class ValidationResult {
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();

        public void addError(String message, String component) {
            errors.add(new ValidationError(message, component));
        }

        public void addWarning(String message, String component) {
            warnings.add(new ValidationWarning(message, component));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<ValidationWarning> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        public int getWarningCount() {
            return warnings.size();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public void printErrors() {
            if (!errors.isEmpty()) {
                logger.error("Found {} validation errors:", errors.size());
                for (int i = 0; i < errors.size(); i++) {
                    ValidationError error = errors.get(i);
                    logger.error("[Error {}/{}] Component: {} - Message: {}",
                            i + 1, errors.size(), error.component, error.message);
                }
            }
        }

        public void printWarnings() {
            if (!warnings.isEmpty()) {
                logger.warn("Found {} validation warnings:", warnings.size());
                for (int i = 0; i < warnings.size(); i++) {
                    ValidationWarning warning = warnings.get(i);
                    logger.warn("[Warning {}/{}] Component: {} - Message: {}",
                            i + 1, warnings.size(), warning.component, warning.message);
                }
            }
        }
    }


    public record ValidationError(String message, String component) {}


    public record ValidationWarning(String message, String component) {}

    private final NodeMetamodelService nodeMetamodelService;

    /**
     * Cache to avoid repeated calls to the catalog for the same node during the validation process
     * of a single workflow
     * TODO: we could implement a cache layer to the NodeMetamodelService itself
     */
    private final Map<String, NodeMetamodel> nodesCache = new HashMap<>();

    /**
     * Constructor that accepts a repository of NodeMetamodels for validation purposes
     */
    public WorkflowMetamodelValidator(NodeMetamodelService nodeMetamodelService) {
        this.nodeMetamodelService = nodeMetamodelService;
    }

    NodeMetamodel getNodeMetamodelById(String nodeId) {
        // Check cache first
        if (nodesCache.containsKey(nodeId)) return nodesCache.get(nodeId);

        // If not in cache, fetch from repository
        Optional<NodeMetamodel> res = nodeMetamodelService.getNodeById(nodeId);
        NodeMetamodel node = res.orElse(null);

        // Save to cache
        nodesCache.put(nodeId, node);

        return node;
    }


    /**
     * Validates a workflow metamodel for correctness
     * @param workflow The workflow metamodel to validate
     * @return Validation result with errors and warnings
     */
    public ValidationResult validate(WorkflowMetamodel workflow) {
        ValidationResult result = new ValidationResult();

        // Empty the cache (for fresh validation)
        nodesCache.clear();


        if (workflow == null) {
            result.addError("Workflow metamodel cannot be null", "workflow");
            return result;
        }

        // Basic validations
        validateBasicProperties(workflow, result);

        // Validate node references
        validateNodeReferences(workflow, result);

        // Validate edge references
        validateEdgeReferences(workflow, result);

        // Validate workflow is a DAG (no cycles)
        validateDAG(workflow, result);

        // Validate port compatibility between ports
        validatePortConnections(workflow, result);

        // Validate entry and exit points
        validateEntryAndExitPoints(workflow, result);

        // Validate edge conditions
        validateEdgeConditions(workflow, result);

        return result;
    }


    /**
     * Validates basic properties of the workflow metamodel
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateBasicProperties(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getId() == null || workflow.getId().isEmpty())
            result.addError("Workflow ID cannot be null or empty", "workflow.id");


        if (workflow.getName() == null || workflow.getName().isEmpty())
            result.addError("Workflow name cannot be null or empty", "workflow.name");


        if (workflow.getNodes() == null || workflow.getNodes().isEmpty())
            result.addError("Workflow must contain at least one node", "workflow.nodes");


        if (workflow.getEdges() == null)
            result.addError("Workflow edges list cannot be null", "workflow.edges");
        else{
            for (var edge : workflow.getEdges()) {
                if (edge.getId() == null || edge.getId().isEmpty()) {
                    result.addError("All workflow edges must have a non-null and non-empty ID", "workflow.edges");
                    break;
                }
            }
        }


    }


    /**
     * Validates that all nodes referenced in the workflow exist in the catalog
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateNodeReferences(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null) return;

        Set<String> nodeIds = new HashSet<>();

        for (WorkflowMetamodel.WorkflowNodeDependency node : workflow.getNodes()) {
            String nodeId = node.getNodeId();

            // Check if node ID is valid
            if (nodeId == null || nodeId.isEmpty()) {
                result.addError("Node ID cannot be null or empty", "workflow.nodes");
                continue;
            }

            // Check for duplicate node IDs
            if (!nodeIds.add(nodeId))
                result.addError("Duplicate node ID found: " + nodeId, "workflow.nodes");


            // Check if node exists in the repository
            if (getNodeMetamodelById(nodeId) == null)
                result.addError("Referenced node does not exist in repository: " + nodeId, "workflow.nodes");
        }
    }

    /**
     * Validates that all edges reference valid nodes
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateEdgeReferences(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Set<String> nodeIds = workflow.getNodes().stream()
                .map(WorkflowMetamodel.WorkflowNodeDependency::getNodeId)
                .collect(Collectors.toSet());

        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (sourceId == null || sourceId.isEmpty())
                result.addError("Edge source node ID cannot be null or empty", "workflow.edges");
             else if (!nodeIds.contains(sourceId))

                result.addError("Edge references non-existent source node: " + sourceId, "workflow.edges");

            if (targetId == null || targetId.isEmpty())
                result.addError("Edge target node ID cannot be null or empty", "workflow.edges");

            else if (!nodeIds.contains(targetId))
                result.addError("Edge references non-existent target node: " + targetId, "workflow.edges");


            // Self-loop check
            if (sourceId != null && sourceId.equals(targetId))
                result.addError("Self-loop detected on node: " + sourceId, "workflow.edges");

        }
    }

    /**
     * Validates that the workflow is a Directed Acyclic Graph (DAG)
     * Uses topological sort to detect cycles
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateDAG(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null || workflow.getEdges() == null) return;

        // Adjacency list
        Map<String, List<String>> adjacencyList = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (WorkflowMetamodel.WorkflowNodeDependency node : workflow.getNodes()) {
            String nodeId = node.getNodeId();
            adjacencyList.put(nodeId, new ArrayList<>());
            inDegree.put(nodeId, 0);
        }

        // Build the adjacency list and in-degree map
        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            if (
                sourceId != null &&
                targetId != null &&
                adjacencyList.containsKey(sourceId) &&
                adjacencyList.containsKey(targetId)
            ) {
                adjacencyList.get(sourceId).add(targetId);
                inDegree.put(targetId, inDegree.get(targetId) + 1);
            }
        }

        // Topological sort
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0)
                queue.add(entry.getKey());


        int visitedCount = 0;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            visitedCount++;

            for (String neighbor : adjacencyList.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                // Add the neighbor to the queue if its in-degree is 0
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);

            }
        }

        // Check for a cycle (If not all nodes were visited, there is a cycle)
        if (visitedCount != workflow.getNodes().size()) {
            result.addError("Cycle detected in workflow graph", "workflow.structure");

            // Nodes in cycles:
            Set<String> nodesInCycles = new HashSet<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() > 0)
                    nodesInCycles.add(entry.getKey());
            }

            result.addError("Nodes involved in cycles: " + String.join(", ", nodesInCycles), "workflow.structure");
        }
    }


    /**
     * Validates that the workflow has a clear entry point and exit points
     * @param workflow Current workflow
     * @param result Validation result
     */
    public void validateEntryAndExitPoints(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null || workflow.getEdges() == null) return;

        Set<String> allNodeIds = workflow
                .getNodes()
                .stream()
                .map(WorkflowMetamodel.WorkflowNodeDependency::getNodeId)
                .collect(Collectors.toSet());

        Set<String> nodesWithIncomingEdges = new HashSet<>();
        Set<String> nodesWithOutgoingEdges = new HashSet<>();

        for (WorkflowEdge edge : workflow.getEdges()) {
            nodesWithIncomingEdges.add(edge.getTargetNodeId());
            nodesWithOutgoingEdges.add(edge.getSourceNodeId());
        }

        // Find entry points (nodes with no incoming edges)
        Set<String> entryPoints = new HashSet<>(allNodeIds);
        entryPoints.removeAll(nodesWithIncomingEdges);

        if (entryPoints.isEmpty())
            result.addError("Workflow has no entry point", "workflow.structure");


        // Find exit points (nodes with no outgoing edges)
        Set<String> exitPoints = new HashSet<>(allNodeIds);
        exitPoints.removeAll(nodesWithOutgoingEdges);

        if (exitPoints.isEmpty())
            result.addError("Workflow has no exit point (all nodes have outgoing edges)", "workflow.structure");
    }


    /**
     * Validates port compatibility between nodes in the workflow
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validatePortConnections(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Map<String, NodeMetamodel> nodesById = new HashMap<>();
        for (WorkflowMetamodel.WorkflowNodeDependency nodeDep : workflow.getNodes()) {
            String nodeId = nodeDep.getNodeId();
            NodeMetamodel node = getNodeMetamodelById(nodeId);
            if (node != null) nodesById.put(nodeId, node);
        }

        Map<String, List<WorkflowEdge>> incomingEdges = new HashMap<>();
        for (WorkflowEdge edge : workflow.getEdges()) {
            String targetId = edge.getTargetNodeId();
            incomingEdges.computeIfAbsent(targetId, k -> new ArrayList<>()).add(edge);
        }

        // Track satisfied required inputs for each node
        Map<String, Set<String>> satisfiedRequiredInputs = new HashMap<>();
        Map<String, Set<String>> allRequiredInputs = new HashMap<>();

        for (String nodeId : nodesById.keySet()) {
            NodeMetamodel node = nodesById.get(nodeId);
            if (node.getInputPorts() == null) continue;

            Set<String> requiredInputs = node.getInputPorts().stream()
                    .filter(port -> port.getSchema() != null && Boolean.TRUE.equals(port.getSchema().getRequired()))
                    .map(Port::getKey)
                    .collect(Collectors.toSet());

            if (!requiredInputs.isEmpty()) {
                allRequiredInputs.put(nodeId, new HashSet<>(requiredInputs));
                satisfiedRequiredInputs.put(nodeId, new HashSet<>());
            }
        }

        // Validate each edge
        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceId = edge.getSourceNodeId();
            String targetId = edge.getTargetNodeId();

            NodeMetamodel sourceNode = nodesById.get(sourceId);
            NodeMetamodel targetNode = nodesById.get(targetId);

            if (sourceNode == null || targetNode == null) continue;

            Map<String, Port> sourceOutputs = sourceNode.getOutputPorts() == null ?
                    Collections.emptyMap() :
                    sourceNode.getOutputPorts().stream().collect(Collectors.toMap(Port::getKey, port -> port));

            Map<String, Port> targetInputs = targetNode.getInputPorts() == null ?
                    Collections.emptyMap() :
                    targetNode.getInputPorts().stream().collect(Collectors.toMap(Port::getKey, port -> port));



            // 1) Explicit bindings first

            Set<String> boundTargetPorts = new HashSet<>();

            if (edge.getBindings() != null && !edge.getBindings().isEmpty()) {
                for (Map.Entry<String, String> binding : edge.getBindings().entrySet()) {
                    String sourceKey = binding.getKey();
                    String targetKey = binding.getValue();

                    if (!sourceOutputs.containsKey(sourceKey)) {
                        result.addError("Source port '" + sourceKey + "' does not exist in node " + sourceId,
                                "workflow.edges." + edge.getId());
                        continue;
                    }

                    if (!targetInputs.containsKey(targetKey)) {
                        result.addError("Target port '" + targetKey + "' does not exist in node " + targetId,
                                "workflow.edges." + edge.getId());
                        continue;
                    }

                    // Check type compatibility
                    Port sourcePort = sourceOutputs.get(sourceKey);
                    Port targetPort = targetInputs.get(targetKey);

                    if (!PortSchema.isCompatible(sourcePort.getSchema(), targetPort.getSchema())) {
                        result.addError("Port type mismatch: " + sourcePort.getSchema().getType() +
                                        " cannot be bound to " + targetPort.getSchema().getType() +
                                        " between nodes " + sourceId + " and " + targetId,
                                "workflow.edges." + edge.getId());
                    }

                    // Mark this target port as bound
                    boundTargetPorts.add(targetKey);

                    // Mark required input as satisfied
                    if (allRequiredInputs.containsKey(targetId) && allRequiredInputs.get(targetId).contains(targetKey))
                        satisfiedRequiredInputs.get(targetId).add(targetKey);

                }
            }

            // 2) Implicit bindings

            // Get all target input ports that haven't been bound yet
            Set<String> unboundTargetInputs = targetInputs.keySet().stream()
                    .filter(port -> !boundTargetPorts.contains(port))
                    .collect(Collectors.toSet());

            // For each unbound target input, check if there's a matching source output
            for (String targetKey : unboundTargetInputs) {
                if (sourceOutputs.containsKey(targetKey)) {
                    Port sourcePort = sourceOutputs.get(targetKey);
                    Port targetPort = targetInputs.get(targetKey);

                    // Check type compatibility
                    if (!PortSchema.isCompatible(sourcePort.getSchema(), targetPort.getSchema())) {
                        result.addWarning("Port type mismatch on implicitly matched port '" + targetKey +
                                        "': " + sourcePort.getSchema().getType() +
                                        " to " + targetPort.getSchema().getType() +
                                        " between nodes " + sourceId + " and " + targetId,
                                "workflow.edges." + edge.getId());
                    } else {
                        // Mark required input as satisfied
                        if (allRequiredInputs.containsKey(targetId) &&
                                allRequiredInputs.get(targetId).contains(targetKey)) {
                            satisfiedRequiredInputs.get(targetId).add(targetKey);
                        }
                    }
                }
            }

            // 3) Check if there are any inputs that are left unsatisfied
            if (allRequiredInputs.containsKey(targetId)) {
                Set<String> required = allRequiredInputs.get(targetId);
                Set<String> satisfied = satisfiedRequiredInputs.get(targetId);

                Set<String> unsatisfied = required.stream()
                        .filter(port -> !satisfied.contains(port))
                        .collect(Collectors.toSet());

                if (!unsatisfied.isEmpty()) {
                    result.addWarning("Node " + targetId + " has unsatisfied required inputs: " +
                            String.join(", ", unsatisfied), "workflow.nodes." + targetId);
                }
            }
        }
    }

    /**
     * Validates that edge conditions reference existing ports with compatible types.
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateEdgeConditions(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Map<String, NodeMetamodel> nodesById = workflow.getNodes().stream()
                .collect(Collectors.toMap(
                        WorkflowMetamodel.WorkflowNodeDependency::getNodeId,
                        node -> getNodeMetamodelById(node.getNodeId()
                )));


        for (WorkflowEdge edge : workflow.getEdges()) {

            System.out.println("Processing edge" + edge.getId() + " with condition: " +
                    (edge.getCondition() != null ? edge.getCondition().getPort() : "null"));

            if (edge.getCondition() == null) continue;

            if (edge.getCondition().getPort() == null) {
                result.addError(
                        "Condition port cannot be null or empty",
                        "workflow.edges." + edge.getId()
                );
                continue;
            }

            if(edge.getCondition().getTargetValue() == null){
                result.addError(
                        "Condition target value cannot be null or empty",
                        "workflow.edges." + edge.getId()
                );
                continue;
            }


            String sourceNodeId = edge.getSourceNodeId();
            String expectedValue = edge.getCondition().getTargetValue();

            NodeMetamodel sourceNode = nodesById.get(sourceNodeId);
            if (sourceNode == null) continue;


            String portKey = edge.getCondition().getPort();



            // Check if the port exists in the source node's outputs
            if (sourceNode.getOutputPorts() == null ||
                    sourceNode.getOutputPorts().stream().noneMatch(p -> p.getKey().equals(portKey))) {

                result.addError(
                        "Condition references non-existent output port '" + portKey +
                                "' in node: " + sourceNodeId,
                        "workflow.edges." + edge.getId()
                );

                continue;
            }

            // Check if the port type is compatible with the expected value
            Optional<Port> port = sourceNode.getOutputPorts().stream()
                    .filter(p -> p.getKey().equals(portKey))
                    .findFirst();

            if (port.isPresent()) {
                PortSchema schema = port.get().getSchema();
                if (schema != null) {
                    boolean isValidValue = validateExpectedValue(schema.getType(), expectedValue);
                    if (!isValidValue) {
                        result.addError(
                                "Condition value '" + expectedValue +
                                        "' is incompatible with port type '" + schema.getType() + "'",
                                "workflow.edges." + edge.getId()
                        );
                    }
                }
            }
        }
    }

    /**
     * Checks if a condition's expected value matches the port's type.
     * @param portType The type of the port
     * @param expectedValue The expected value to validate
     */
    private boolean validateExpectedValue(PortType portType, String expectedValue) {
        if (expectedValue == null) return false;

        try {
            return switch (portType) {
                case BOOLEAN -> expectedValue.equalsIgnoreCase("true") ||
                        expectedValue.equalsIgnoreCase("false");
                case INT -> {
                    Integer.parseInt(expectedValue);
                    yield true;
                }
                case FLOAT -> {
                    Float.parseFloat(expectedValue);
                    yield true;
                }
                case STRING -> true;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

}