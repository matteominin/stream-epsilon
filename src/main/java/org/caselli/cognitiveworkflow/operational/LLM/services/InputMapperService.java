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


    public InputMapperResult mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes) {
        return mapInput(variables, nodes, null);
    }


    /**
     * Maps input variables to the input ports of the workflow starting nodes using an LLM.
     *
     * @param variables    Map of unstructured variables
     * @param nodes        List of starting workflow nodes
     * @param requestInput Request of the user in natural language
     * @return InputMapperResult containing the found variable-to-port bindings,
     * or null if no suitable mapping could be determined
     */
    public InputMapperResult mapInput(Map<String, Object> variables, List<NodeMetamodel> nodes, String requestInput) {
        logger.info("Starting input mapping with LLM for {} variables and {} nodes", variables.size(), nodes.size());


        if (nodes.isEmpty()) {
            logger.warn("No nodes available for mapping");
            return null;
        }

        if (variables.isEmpty() && (requestInput == null || requestInput.isEmpty())) {
            logger.info("No variables provided, skipping mapping");
            return new InputMapperResult(new ExecutionContext());
        }


        try {
            String nodesDescription = buildNodesDescription(nodes);
            String variablesDescription = buildVariablesDescription(variables);
            String requestDescription = "<request_text>" + requestInput + "</request_text>";

            // Build the prompt
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_INSTRUCTIONS),
                    new UserMessage(variablesDescription + "\n\n" + nodesDescription + "\n\n" + requestDescription)
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
     *
     * @param nodes   List of nodes to validate against
     * @param context ExecutionContext containing the generated bindings
     */
    private boolean isGeneratedContextValid(List<NodeMetamodel> nodes, ExecutionContext context) {
        if (context == null) return false;

        for (var node : nodes) {
            for (var port : node.getInputPorts())
                if (
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
            You are a **Data Population System** for workflow node inputs. Your task is to populate the input ports of workflow nodes using **ONLY** the information available in the user request and provided variables.

            ---

            # INPUT SOURCES
            You will be provided with:
            - **User variables**: Key-value pairs in the `<user_variables>` section.
            - **Workflow nodes**: Definitions with input port schemas in the `<nodes_list>` section.
            - **User request**: The original textual input in the `<request_text>` section (if present).

            ---

            # PROCESS STEPS

            ## 1. Information Extraction
            Thoroughly extract **all relevant data points** from both the `<user_variables>` and `<request_text>` sections. Identify concepts, entities, and values that directly align with the requirements of potential input ports.

            ## 2. Data Population & Binding
            Examine each workflow node and its defined input ports. For every port, especially **required** ones, attempt to find a corresponding value from the extracted information in Step 1.
            - **For descriptive string ports (e.g., `STRING` type ports expecting a query or description):** If the user request contains a phrase, query, or statement that directly describes the port's purpose, extract that *entire relevant portion* of the user request as the value.
            - If a suitable value is found, create a binding according to the `MAPPING SYNTAX`.
            - **Prioritize populating required ports.**

            ---

            # RULES

            ## Allowed Data Transformations:
            You **MAY** transform available data through:
            - **String Manipulation**: Extracting substrings, splitting text, parsing numbers.
            - **Format Conversion**: Converting dates, numbers, or boolean representations.
            - **Unit Conversion**: Converting measurements (e.g., kg to lbs, EUR to USD) **ONLY** if conversion rates are explicitly provided or are commonly known.
            - **Data Structuring**: Organizing flat data into nested objects or arrays.
            - **Type Casting**: Converting strings to numbers, booleans, etc., to match the port's `primitiveType`.

            ## Forbidden Actions:
            You **MUST NOT**:
            - Invent or hallucinate any missing values.
            - Make assumptions about unstated information.
            - Use default values unless explicitly provided in the input.
            - Extrapolate beyond the exact data available.
            - Create placeholder or example data.
            - **If a port cannot be satisfied with existing information without inventing data, you MUST leave it unsatisfied.**

            ---

            # MAPPING SYNTAX
            - **Simple Mapping**: `"port_name": "extracted_value"`
            - **Nested Objects**: `"parent.child": "value"`, e.g., `"customer.name": "John Doe"`
            - **Array Elements**: `"array_name.index": "value"`, e.g., `"features.0": "5G connectivity"`
            - **Value Types**: Values must be primitive types: strings, numbers, or booleans. **Do NOT generate objects or arrays as values.**

            ---

            # OUTPUT FORMAT
            Return a JSON object with a single top-level key named `bindings`. The value of `bindings` must be a map (JSON object) containing the successfully populated port-to-value mappings. If no ports can be populated, return an empty `bindings` map.

            **Success Example:**
            ```json
            {
              "bindings": {
                "product_name": "iPhone 15",
                "price": 999.99,
                "customer.name": "Mario Rossi",
                "features.0": "5G connectivity",
                "features.1": "Face ID"
              }
            }
            ```

            ---

            # EXAMPLES OF FORBIDDEN ACTIONS
            - **Scenario**: User provides "name: Mario" but a port requires "email."
              **Forbidden**: Do NOT invent an email address.
            - **Scenario**: User provides a partial address, but a port requires a complete address.
              **Forbidden**: Do NOT fill in missing parts.
            - **Scenario**: User provides a product name, but a port requires the price.
              **Forbidden**: Do NOT generate a price.
            - **Scenario**: Required data is missing.
              **Forbidden**: Do NOT use placeholders like "TBD" or "unknown."
            """;
}