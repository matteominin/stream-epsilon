package org.caselli.cognitiveworkflow.operational.LLM;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * LLM-based service for mapping input variables to workflow nodes.
 * This service analyzes unstructured variables and determines the most suitable starting node
 * in a workflow, mapping the variables to the node input structure.
 */
@Service
public class InputMapperService {
    private static final Logger logger = LoggerFactory.getLogger(InputMapperService.class);

    private final LlmModelFactory llmModelFactory;
    private ChatClient chatClient;

    @Value("${input-mapper.llm.provider:}")
    private String intentProvider;

    @Value("${input-mapper.llm.api-key:}")
    private String intentApiKey;

    @Value("${input-mapper.llm.model:}")
    private String intentModel;

    @Value("${port-adapter.llm.temperature}")
    private double temperature;

    public InputMapperService(LlmModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    /**
     * Maps input variables to the most suitable workflow starting node using LLM.
     *
     * @param variables Map of unstructured variables
     * @param nodes List of available workflow nodes to consider for mapping
     * @return InputMapperResult containing the selected node and variable bindings,
     *         or null if no suitable mapping could be determined
     */
    public InputMapperResult mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes) {
        logger.info("Starting input mapping with LLM for {} variables and {} nodes", variables.size(), nodes.size());

        if (nodes.isEmpty()) {
            logger.warn("No nodes available for mapping");
            return null;
        }

        if (variables.isEmpty()) {
            logger.info("No variables provided, skipping mapping");
            return new InputMapperResult(nodes.get(0), new ExecutionContext());
        }

        try {
            String nodesDescription = buildNodesDescription(nodes);
            String variablesDescription = buildVariablesDescription(variables);

            // Build the prompt
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_INSTRUCTIONS),
                    new UserMessage(variablesDescription + "\n\n" + nodesDescription)
            ));


            // Call the LLM
            InputMapperLLMResult result = getChatClient()
                    .prompt(prompt)
                    .call()
                    .entity(InputMapperLLMResult.class);

            // TODO: remove
            System.out.println(prompt.getContents());
            System.out.println(result);

            return processLLMResult(result, nodes);
        } catch (Exception e) {
            logger.error("Input mapping failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildNodesDescription(List<NodeMetamodel> nodes) {
        StringBuilder builder = new StringBuilder("<available_nodes_list>\n");

        nodes.forEach(node -> {
            String portsDescription = node.getInputPorts().stream()
                    .map(Port::portToJson)
                    .map(s -> s.replace("{", "\\{").replace("}", "\\}"))
                    .collect(Collectors.joining(",\n    "));

            builder.append("- Node ID: ").append(node.getId()).append("\n")
                    .append("- Name: ").append(node.getName()).append("\n")
                    .append("- Description: ").append(node.getDescription()).append("\n")
                    .append("  Input Ports:\n")
                    .append("  ```json\n")
                    .append("  [\n    ").append(portsDescription).append("\n  ]\n")
                    .append("  ```\n\n");
        });

        return builder.append("</available_nodes_list>").toString();
    }

    private String buildVariablesDescription(Map<String, Object> variables) {
        StringBuilder builder = new StringBuilder("<user_variables>\n");
        variables.forEach((key, value) ->
                builder.append("- ").append(key).append(": ").append(value).append("\n")
        );
        return builder.append("</user_variables>").toString();
    }

    private InputMapperResult processLLMResult(InputMapperLLMResult llmResult, List<NodeMetamodel> nodes) {
        if (llmResult == null || !StringUtils.hasText(llmResult.getSelectedStartingNodeId())) {
            logger.warn("No suitable node identified by LLM");
            return null;
        }

        // Find the node by the ID provided by the LLM
        NodeMetamodel startingNode = nodes.stream()
                .filter(node -> node.getId().equals(llmResult.getSelectedStartingNodeId()))
                .findFirst()
                .orElse(null);

        // Fallback if the LLM has returned a non-existing ID
        if (startingNode == null) {
            logger.error("LLM selected invalid node ID: {}", llmResult.getSelectedStartingNodeId());
            return null;
        }

        // Create the context
        ExecutionContext context = new ExecutionContext();
        if (llmResult.getBindings() != null) context.putAll(llmResult.getBindings());

        // Final check to see if the LLM response is valid
        if (!isGeneratedContextValid(startingNode, context)) {
            logger.error("The LLM provided an input that do not satisfy the required ports of the  selected node ID: {}", llmResult.getSelectedStartingNodeId());
            return null;
        }

        return new InputMapperResult(startingNode, context);
    }

    private ChatClient getChatClient() {
        if (chatClient == null) {
            validateLlmConfiguration();
            var options = new LlmModelFactory.BaseLlmModelOptions();
            options.setTemperature(temperature);
            chatClient = llmModelFactory.createChatClient(intentProvider, intentApiKey, intentModel, options);
        }
        return chatClient;
    }

    private void validateLlmConfiguration() {
        if (!StringUtils.hasText(intentProvider) ||
                !StringUtils.hasText(intentApiKey) ||
                !StringUtils.hasText(intentModel)) {
            throw new IllegalArgumentException("LLM configuration (provider, api-key, model) is missing or incomplete"
            );
        }
    }

    /**
     * Validates that all required ports for a node are satisfied by the provided bindings of the LLM
     */
    private boolean isGeneratedContextValid(NodeMetamodel node, ExecutionContext context) {
        if (context == null) return false;

        for (var port : node.getInputPorts())
            if(
                port.getSchema().getRequired() != null &&
                port.getSchema().getRequired() &&
                !port.getSchema().isValidValue(context.get(port.getKey()))
            )
                return false;
        return true;
    }

    /**
     * Helper class for structured LLM calls for Input Mapping
     */
    @Data
    private static class InputMapperLLMResult {
        @JsonProperty(required = true)
        String selectedStartingNodeId;

        @JsonProperty(required = true)
        Map<String, String> bindings;
    }

    /**
     * InputMapperService response
     */
    @Data
    @AllArgsConstructor
    public static class InputMapperResult {
        NodeMetamodel startingNode;
        ExecutionContext context;
    }

    private static final String SYSTEM_INSTRUCTIONS =
            """
            # ROLE
            You are an expert Input Mapping System for workflow orchestration. Your task is to:
            1. Analyze user-provided variables
            2. Match them to the most suitable workflow starting node
            3. Generate precise variable-to-port mappings
    
            # INPUT FORMAT
            You will receive:
            - User variables in <user_variables> section
            - Available nodes in <available_nodes_list> section
    
            # PROCESSING RULES
            1. FIRST identify all nodes whose REQUIRED input ports can be fully satisfied by the user variables
            2. THEN select the most appropriate starting node based on:
               - Semantic matching of variable names/values to port descriptions
               - Node purpose alignment with variable context
               - Completeness of required port coverage
            3. FINALLY create exact mappings between user variables and node input ports
    
            # MAPPING REQUIREMENTS
            - ONLY map variables that DIRECTLY correspond to port requirements
            - Use dot notation for nested structures (e.g., "address.city")
            - PRESERVE original variable values - DO NOT transform or invent data
            - PRIORITIZE required ports over optional ones
            - REJECT mapping if required ports cannot be satisfied
    
            # OUTPUT FORMAT
            Return JSON with:
            - selectedStartingNodeId: The ID of the chosen node
            - bindings: Map of port paths to variable values
              Example: {"user_input.name": "customerName"}
    
            # ERROR HANDLING
            If no suitable node is found, return empty selectedStartingNodeId
            """;
}