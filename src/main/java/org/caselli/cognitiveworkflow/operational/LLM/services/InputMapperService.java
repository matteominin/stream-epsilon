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
            1. Analyze user-provided variables.
            2. Match them to the inputs of ALL the workflow initial nodes.
            3. Generate precise variable-to-port mappings that satisfy the required inputs of ALL initial nodes.
            4. If any required input for any initial node cannot be satisfied, then no mapping should be provided.

            # INPUT FORMAT
            You will receive:
            - User variables in <user_variables> section.
            - Initial nodes in <nodes_list> section.

            # PROCESSING RULES
            1. Analyze the input ports of ALL nodes in the <nodes_list> section.
            2. For each initial node, identify its required input ports.
            3. Determine if the user-provided variables can collectively satisfy ALL required input ports across ALL initial nodes.
            4. If all required ports across all initial nodes can be satisfied, create a single set of bindings that maps port paths to the *actual values* of the corresponding user variables.
            5. If any required port for any initial node cannot be satisfied by the user variables, you must NOT provide any bindings.

            # MAPPING REQUIREMENTS
            - ONLY map variables that DIRECTLY correspond to port requirements.
            - Use dot notation for nested structures (e.g., "address.city").
            - PRESERVE original variable values - DO NOT transform or invent data.
            - PRIORITIZE required ports over optional ones.

            # OUTPUT FORMAT
            Return JSON with:
            - bindings: A single map where keys are port paths (e.g., "input_param_a") and values are the *actual content* of the mapped user variables (e.g., "John Doe", "123 Main St"). This map must satisfy all initial nodes' required inputs in a shared context.

            Example:
            ```json
            {
              "bindings": {
                "product_type": "iPhone 15 Pro",
                "product_price": 999.99,
                "customer_name": "John Doe",
              }
            }
            ```

            # ERROR HANDLING
            If no suitable mappings are found or are possible (i.e., required inputs for all initial nodes cannot be satisfied), return an empty JSON object ({}).
            """;
}