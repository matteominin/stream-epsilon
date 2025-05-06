package org.caselli.cognitiveworkflow.knowledge.validation;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
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

    private final NodeMetamodelService nodeMetamodelService;


    public WorkflowMetamodelValidator(NodeMetamodelService nodeMetamodelService) {
        this.nodeMetamodelService = nodeMetamodelService;
    }

    /**
     * Helper method to get a NodeMetamodel by its ID
     * @param nodeId The ID of the node to retrieve
     * @return The NodeMetamodel if found, null otherwise
     */
    private NodeMetamodel getNodeMetamodelById(String nodeId) {
        Optional<NodeMetamodel> res = nodeMetamodelService.getNodeById(nodeId);
        return res.orElse(null);
    }

    /**
     * Validates a workflow metamodel for correctness
     * @param workflow The workflow metamodel to validate
     * @return Validation result with errors and warnings
     */
    public ValidationResult validate(WorkflowMetamodel workflow) {
        ValidationResult result = new ValidationResult();

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

        for (WorkflowNode node : workflow.getNodes()) {
            String nodeId = node.getNodeMetamodelId();

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
                .map(WorkflowNode::getId)
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

        for (WorkflowNode node : workflow.getNodes()) {
            String nodeId = node.getNodeMetamodelId();
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
                .map(WorkflowNode::getNodeMetamodelId)
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
     * Validates that edge conditions reference existing ports with compatible types.
     * @param workflow Current workflow
     * @param result Validation result
     */
    private void validateEdgeConditions(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getEdges() == null || workflow.getNodes() == null) return;

        Map<String, NodeMetamodel> nodesById = workflow.getNodes().stream()
                .filter(node -> node.getNodeMetamodelId() != null)
                .filter(node -> getNodeMetamodelById(node.getNodeMetamodelId()) != null)
                .collect(Collectors.toMap(
                        WorkflowNode::getNodeMetamodelId,
                        node -> getNodeMetamodelById(node.getNodeMetamodelId())
                ));

        for (WorkflowEdge edge : workflow.getEdges()) {
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
            Optional<? extends Port> port = sourceNode.getOutputPorts().stream()
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



    /**
     * Helper method to retrieve and resolve a PortSchema given a full path (e.g., "basePort.nestedField").
     * It uses PortSchema.getSchemaByPath() for resolving nested parts.
     */
    private PortSchema getResolvedSchemaForPort(NodeMetamodel nodeMetamodel, String fullPathKey, String direction,
                                                ValidationResult result, String edgeId, String workflowNodeId) {
        String edgeIdForContext = (edgeId == null ? "[UNIDENTIFIED_EDGE]" : edgeId);
        String nodeContextErrorPrefix = direction + " port path '" + fullPathKey + "' on NodeMetamodel ID '" + nodeMetamodel.getId() +
                "' (WorkflowNode ID: '" + workflowNodeId + "', Edge ID: '" + edgeIdForContext + "'): ";
        String errorFieldKey = "workflow.edges." + edgeIdForContext;


        if (fullPathKey == null || fullPathKey.isEmpty()) {
            result.addError(nodeContextErrorPrefix + "Path key cannot be null or empty.", errorFieldKey);
            return null;
        }

        // Split into base port key and the rest of the path (if any)
        // "port.field.sub" -> parts[0]="port", parts[1]="field.sub"
        // "port"           -> parts[0]="port", parts.length=1
        String[] parts = fullPathKey.split("\\.", 2);
        String basePortKey = parts[0];
        String nestedPath = (parts.length > 1 && parts[1] != null && !parts[1].isEmpty()) ? parts[1] : null;

        if (basePortKey.isEmpty()) { // Handles cases like ".field" or leading dot
            result.addError(nodeContextErrorPrefix + "Base port key part of the path cannot be empty.", errorFieldKey);
            return null;
        }

        List<? extends Port> ports = (Objects.equals(direction, "INPUT")) ? nodeMetamodel.getInputPorts() : nodeMetamodel.getOutputPorts();
        if (ports == null) { // Should ideally be an empty list, not null, from NodeMetamodel
            ports = Collections.emptyList();
        }

        Optional<? extends Port> basePortOpt = ports.stream()
                .filter(p -> p.getKey() != null && p.getKey().equals(basePortKey))
                .findFirst();

        if (basePortOpt.isEmpty()) {
            result.addError(nodeContextErrorPrefix + "Base port key '" + basePortKey + "' does not exist.", errorFieldKey);
            return null;
        }

        Port basePort = basePortOpt.get();
        PortSchema baseSchema = basePort.getSchema();

        if (baseSchema == null) {
            result.addError(nodeContextErrorPrefix + "Base port '" + basePortKey + "' has no schema defined.", errorFieldKey);
            return null;
        }

        // If there's a nested path, resolve it using the base schema's method
        if (nestedPath != null) {
            try {
                return baseSchema.getSchemaByPath(nestedPath);
            } catch (Exception e) {
                result.addError(nodeContextErrorPrefix + "Invalid nested path '" + nestedPath + "' within schema of base port '" + basePortKey + "'. Reason: " + e.getMessage(), errorFieldKey);
                return null;
            }
        } else {
            // No nested path, the schema is the base port's schema itself
            return baseSchema;
        }
    }

    /**
     * Validates port connections, including type compatibility and satisfaction of required inputs,
     * supporting dot-notation for bindings.
     */
    private void validatePortConnections(WorkflowMetamodel workflow, ValidationResult result) {
        if (workflow.getNodes() == null || workflow.getEdges() == null) return;

        Map<String, NodeMetamodel> workflowNodeIdToMetamodel = new HashMap<>();
        Map<String, WorkflowNode> workflowNodesMap = new HashMap<>(); // For easy access to WorkflowNode specific info

        for (WorkflowNode wn : workflow.getNodes()) {
            if (wn.getId() != null && !wn.getId().isEmpty()) {
                workflowNodesMap.put(wn.getId(), wn);
                if (wn.getNodeMetamodelId() != null && !wn.getNodeMetamodelId().isEmpty()) {
                    NodeMetamodel nmm = getNodeMetamodelById(wn.getNodeMetamodelId());
                    if (nmm != null) {
                        workflowNodeIdToMetamodel.put(wn.getId(), nmm);
                    }
                    // Error for missing NMM already covered by validateNodeReferences
                }
            }
        }

        Map<String, Set<String>> satisfiedRequiredInputs = new HashMap<>(); // Key: WorkflowNode.id
        Map<String, Set<String>> allRequiredInputs = new HashMap<>();       // Key: WorkflowNode.id

        for (WorkflowNode wn : workflow.getNodes()) {
            String wnId = wn.getId();
            if (wnId == null || wnId.isEmpty()) continue;
            NodeMetamodel nmm = workflowNodeIdToMetamodel.get(wnId);
            if (nmm == null || nmm.getInputPorts() == null) continue;

            Set<String> requiredBaseInputs = nmm.getInputPorts().stream()
                    .filter(port -> port.getKey() != null && port.getSchema() != null && Boolean.TRUE.equals(port.getSchema().getRequired()))
                    .map(Port::getKey)
                    .collect(Collectors.toSet());

            if (!requiredBaseInputs.isEmpty()) {
                allRequiredInputs.put(wnId, requiredBaseInputs);
                satisfiedRequiredInputs.put(wnId, new HashSet<>());
            }
        }

        for (WorkflowEdge edge : workflow.getEdges()) {
            String sourceWorkflowNodeId = edge.getSourceNodeId();
            String targetWorkflowNodeId = edge.getTargetNodeId();
            String edgeId = edge.getId();
            String edgeContext = "workflow.edges." + (edgeId == null ? "[UNIDENTIFIED_EDGE]" : edgeId);

            if (sourceWorkflowNodeId == null || targetWorkflowNodeId == null ||
                    !workflowNodeIdToMetamodel.containsKey(sourceWorkflowNodeId) ||
                    !workflowNodeIdToMetamodel.containsKey(targetWorkflowNodeId)) {
                // Errors for invalid edge node IDs or missing metamodels already caught.
                continue;
            }

            NodeMetamodel sourceNodeMetamodel = workflowNodeIdToMetamodel.get(sourceWorkflowNodeId);
            NodeMetamodel targetNodeMetamodel = workflowNodeIdToMetamodel.get(targetWorkflowNodeId);

            Set<String> boundTargetBasePortKeysInEdge = new HashSet<>(); // Tracks base target ports explicitly bound in this edge

            // 1. Explicit Bindings
            if (edge.getBindings() != null && !edge.getBindings().isEmpty()) {
                for (Map.Entry<String, String> binding : edge.getBindings().entrySet()) {
                    String sourceFullPath = binding.getKey();
                    String targetFullPath = binding.getValue();

                    PortSchema actualSourceSchema = getResolvedSchemaForPort(sourceNodeMetamodel, sourceFullPath, "OUTPUT", result, edgeId, sourceWorkflowNodeId);
                    PortSchema actualTargetSchema = getResolvedSchemaForPort(targetNodeMetamodel, targetFullPath,"INPUT", result, edgeId, targetWorkflowNodeId);

                    if (actualSourceSchema != null && actualTargetSchema != null) {
                        if (!PortSchema.isCompatible(actualSourceSchema, actualTargetSchema)) {
                            result.addError("Port type mismatch on explicit binding: Source '" + sourceFullPath + "' (type: " + actualSourceSchema.getType() +
                                            ") is incompatible with target '" + targetFullPath + "' (type: " + actualTargetSchema.getType() + "). " +
                                            "Edge: '" + edgeId + "', WorkflowNodes: '" + sourceWorkflowNodeId + "' -> '" + targetWorkflowNodeId + "'.",
                                    edgeContext + ".binding.typeMismatch");
                        }
                        // Mark the base target port as having a binding and potentially satisfying requirement
                        String baseTargetKey = targetFullPath.split("\\.", 2)[0];
                        if (allRequiredInputs.containsKey(targetWorkflowNodeId) && allRequiredInputs.get(targetWorkflowNodeId).contains(baseTargetKey)) {
                            satisfiedRequiredInputs.get(targetWorkflowNodeId).add(baseTargetKey);
                        }
                        boundTargetBasePortKeysInEdge.add(baseTargetKey);
                    }
                }
            }

            // 2. Implicit Bindings (only for top-level ports not explicitly bound in this edge)
            if (targetNodeMetamodel.getInputPorts() != null && sourceNodeMetamodel.getOutputPorts() != null) {
                Map<String, Port> targetInputBasePorts = targetNodeMetamodel.getInputPorts().stream()
                        .filter(p -> p.getKey() != null)
                        .collect(Collectors.toMap(Port::getKey, p -> p, (p1, p2) -> p1)); // Handle duplicates if any, take first
                Map<String, Port> sourceOutputBasePorts = sourceNodeMetamodel.getOutputPorts().stream()
                        .filter(p -> p.getKey() != null)
                        .collect(Collectors.toMap(Port::getKey, p -> p, (p1, p2) -> p1));

                for (Map.Entry<String, Port> targetPortEntry : targetInputBasePorts.entrySet()) {
                    String targetBaseKey = targetPortEntry.getKey();
                    Port targetBasePort = targetPortEntry.getValue();

                    // Consider for implicit binding only if not explicitly bound in this edge
                    if (!boundTargetBasePortKeysInEdge.contains(targetBaseKey) && sourceOutputBasePorts.containsKey(targetBaseKey)) {
                        Port sourceBasePort = sourceOutputBasePorts.get(targetBaseKey);
                        if (sourceBasePort.getSchema() != null && targetBasePort.getSchema() != null) {
                            if (!PortSchema.isCompatible(sourceBasePort.getSchema(), targetBasePort.getSchema())) {
                                result.addWarning("Port type mismatch on implicitly matched port '" + targetBaseKey + "': Source type " + sourceBasePort.getSchema().getType() +
                                                " to target type " + targetBasePort.getSchema().getType() + ". Edge: '" + edgeId + "', WorkflowNodes: '" + sourceWorkflowNodeId + "' -> '" + targetWorkflowNodeId + "'.",
                                        edgeContext + ".implicitBinding.typeMismatch");
                            } else {
                                // Mark required input as satisfied by implicit binding
                                if (allRequiredInputs.containsKey(targetWorkflowNodeId) && allRequiredInputs.get(targetWorkflowNodeId).contains(targetBaseKey)) {
                                    satisfiedRequiredInputs.get(targetWorkflowNodeId).add(targetBaseKey);
                                }
                            }
                        } else {
                            // Warning if schemas are null for implicitly matched ports
                            if (sourceBasePort.getSchema() == null) result.addWarning("Source port '" + targetBaseKey + "' in implicit match has no schema. Node: " + sourceWorkflowNodeId, edgeContext);
                            if (targetBasePort.getSchema() == null) result.addWarning("Target port '" + targetBaseKey + "' in implicit match has no schema. Node: " + targetWorkflowNodeId, edgeContext);
                        }
                    }
                }
            }
        }

        // 3. Final check for unsatisfied required inputs on each node instance
        for (Map.Entry<String, Set<String>> entry : allRequiredInputs.entrySet()) {
            String workflowNodeId = entry.getKey();
            Set<String> required = entry.getValue();
            Set<String> satisfied = satisfiedRequiredInputs.getOrDefault(workflowNodeId, Collections.emptySet());
            Set<String> unsatisfied = new HashSet<>(required); // Start with all required
            unsatisfied.removeAll(satisfied); // Remove those that were satisfied

            if (!unsatisfied.isEmpty()) {
                WorkflowNode wn = workflowNodesMap.get(workflowNodeId); // Get the WorkflowNode for context
                String nmmId = (wn != null && wn.getNodeMetamodelId() != null) ? wn.getNodeMetamodelId() : "[UNKNOWN_METAMODEL]";
                result.addWarning("WorkflowNode ID '" + workflowNodeId + "' (Metamodel ID: '" + nmmId + "') has unsatisfied required input port(s): " +
                        String.join(", ", unsatisfied), "workflow.nodes." + workflowNodeId + ".requiredInputs.unsatisfied");
            }
        }
    }


}
