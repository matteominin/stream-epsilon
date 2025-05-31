package org.caselli.cognitiveworkflow.operational.LLM.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.LLMAbstractService;
import org.caselli.cognitiveworkflow.operational.LLM.factories.LLMModelFactory;
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
public class InputMapperService extends LLMAbstractService {
    private final LLMModelFactory llmModelFactory;

    @Value("${input-mapper.llm.provider:}")
    private String provider;

    @Value("${input-mapper.llm.api-key:}")
    private String apiKey;

    @Value("${input-mapper.llm.model:}")
    private String model;

    @Value("${port-adapter.llm.temperature}")
    private double temperature;

    public InputMapperService(LLMModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    /**
     * Maps input variables to the input ports of the workflow starting nodes using an LLM.
     *
     * @param variables Map of unstructured variables
     * @param nodes List of starting workflow nodes
     * @return InputMapperResult containing the found variable-to-port bindings,
     *          or null if no suitable mapping could be determined
     */
    public InputMapperResult mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes) {
        logger.info("Starting input mapping with LLM for {} variables and {} nodes", variables.size(), nodes.size());


        if (nodes.isEmpty()) {
            logger.warn("No nodes available for mapping");
            return null;
        }

        if (variables.isEmpty()) {
            logger.info("No variables provided, skipping mapping");
            return new InputMapperResult(new ExecutionContext());
        }


        try {
            String nodesDescription = buildNodesDescription(nodes);
            String variablesDescription = buildVariablesDescription(variables);

            // Build the prompt
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_INSTRUCTIONS),
                    new UserMessage(variablesDescription + "\n\n" + nodesDescription)
            ));


            // TODO: remove this debug line in production
            System.out.println("Prompt: " + prompt.getContents());


            // Call the LLM
            InputMapperLLMResult result = getChatClient()
                    .prompt(prompt)
                    .call()
                    .entity(InputMapperLLMResult.class);


            logger.info("LLM returned {}", result != null ? result.getBindings() : "null");

            return processLLMResult(result, nodes);
        } catch (Exception e) {
            logger.error("Input mapping failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildNodesDescription(List<NodeMetamodel> nodes) {
        StringBuilder builder = new StringBuilder("<nodes_list>\n");

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

        return builder.append("</nodes_list>").toString();
    }

    private String buildVariablesDescription(Map<String, Object> variables) {
        StringBuilder builder = new StringBuilder("<user_variables>\n");
        variables.forEach((key, value) ->
                builder.append("- ").append(key).append(": ").append(value).append("\n")
        );
        return builder.append("</user_variables>").toString();
    }

    private InputMapperResult processLLMResult(InputMapperLLMResult llmResult, List<NodeMetamodel> nodes) {
        if (llmResult == null || llmResult.getBindings() == null || llmResult.getBindings().isEmpty()) {
            logger.warn("LLM returned null result");

            // check if the input ports of all the initial nodes are satisfied (i.e., no required ports)
            if (nodes.stream().allMatch(node -> node.getInputPorts().stream().noneMatch(port -> port.getSchema().getRequired() != null && port.getSchema().getRequired()))) {
                return new InputMapperResult(new ExecutionContext());
            }

            logger.warn("No suitable variable-to-port can be determined for the provided nodes and variables");

            return null;
        }

        // Create the context
        ExecutionContext context = new ExecutionContext();
        if (llmResult.getBindings() != null) context.putAll(llmResult.getBindings());

        // Final check to see if the LLM response is valid
        if (!isGeneratedContextValid(nodes, context)) {
            logger.error("The LLM provided an input that do not satisfy the required ports of all the initial nodes");
            return null;
        }

        return new InputMapperResult(context);
    }
    private void validateLlmConfiguration() {
        if (!StringUtils.hasText(provider) ||
                !StringUtils.hasText(apiKey) ||
                !StringUtils.hasText(model)) {
            throw new IllegalArgumentException("LLM configuration (provider, api-key, model) is missing or incomplete"
            );
        }
    }

    @Override
    protected ChatClient buildChatClient() {
        validateLlmConfiguration();

        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(temperature);
        return llmModelFactory.createChatClient(provider, model, apiKey, options);
    }


    /**
     * Validates that all required ports of the nodes are satisfied by the generated bindings
     * @param nodes List of nodes to validate against
     * @param context ExecutionContext containing the generated bindings
     */
    private boolean isGeneratedContextValid(List<NodeMetamodel> nodes, ExecutionContext context) {
        if (context == null) return false;

        for (var node : nodes){
            for (var port : node.getInputPorts())
                if(
                        port.getSchema().getRequired() != null &&
                                port.getSchema().getRequired() &&
                                !port.getSchema().isValidValue(context.get(port.getKey()))
                )
                    return false;
        }

        return true;
    }

    /**
     * Helper class for structured LLM calls for Input Mapping
     */
    @Data
    private static class InputMapperLLMResult {
        @JsonProperty(required = true)
        Map<String, String> bindings;
    }

    /**
     * InputMapperService response
     */
    @Data
    @AllArgsConstructor
    public static class InputMapperResult {
        ExecutionContext context;
    }

    private static final String SYSTEM_INSTRUCTIONS =
            """
            # ROLE
            You are an expert Input Mapping System for workflow orchestration. Your task is to:
            1. Analyze user-provided variables and initial node inputs.
            2. Create precise variable-to-port mappings for ALL required inputs of ALL initial nodes.
            3. If any required input for any initial node cannot be satisfied by the user variables, provide no mappings.
    
            # INPUT FORMAT
            You will receive:
            - User variables in the <user_variables> section.
            - Initial nodes in the <nodes_list> section.
    
            # PROCESSING RULES
            1. Examine the input ports of ALL nodes in the <nodes_list> section.
            2. Identify all *required* input ports for each initial node.
            3. Determine if the user-provided variables can *collectively satisfy every single required input port* across *all* initial nodes.
            4. If, and only if, all required ports across all initial nodes can be satisfied, then generate a single set of bindings.
            5. Aggregation: If a required input port expects an array and multiple user variables semantically represent individual elements of that array, you may combine these relevant user variables into a single array mapping
    
    
            # MAPPING REQUIREMENTS
            - **Direct Correspondence:** ONLY map variables that DIRECTLY correspond to port requirements.
            - **Dot Notation:** Use dot notation for nested structures (e.g., "address.city").
            - **Array Dot Notation:** For array inputs, map individual elements using array dot notation (e.g., "items.0", "items.1"). The value mapped must be the actual content.
            - **Preserve Values:** PRESERVE original variable values; DO NOT transform, invent, or combine data into new forms unless explicitly by dot notation for nested/array structures.
            - **Prioritize Required:** prioritize required ports over optional ones. However if optional ports can be satisfied, we MUST include them in the mapping, otherwise the user variables will not be used.
    
            # OUTPUT FORMAT
            Return JSON with a 'bindings' map:
            - `bindings`: A single map where keys are string port paths (e.g., "input_param_a", "customer.address.city", "items.0") and values are the *actual content* of the mapped user variables (e.g., "John Doe", "New York", "first_item_value").
            - Keys must be strings (using dot notation for nested objects and array elements).
            - Values must be primitive types (strings, numbers, booleans - not JSON objects or arrays).
    
            Example for general mapping:
            ```json
            {
              "bindings": {
                "product_type": "iPhone 15 Pro",
                "product_price": 999.99,
                "customer_name": "John Doe"
              }
            }
            ```
            Example for nested objects:
            ```json
            {
              "bindings": {
                "customer.address.city": "New York",
                "customer.address.zip": "10001"
              }
            }
            ```
            Example for mapping array elements:
            ```json
            {
              "bindings": {
                "requirements.0": "real-time connection",
                "requirements.1": "low latency",
                "requirements.2": "4K resolution"
              }
            }
            ```
    
            # ERROR HANDLING
            If no suitable mappings are found (i.e., not all required inputs can be satisfied), return an empty JSON object: `{}`.
            """;
}