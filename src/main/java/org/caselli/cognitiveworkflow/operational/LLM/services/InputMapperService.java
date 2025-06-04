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
import java.util.Optional;
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
                    You are a Data Population System for workflow node inputs. Your task is to populate the input ports of workflow nodes using ONLY the information available in user variables and natural language requests.
                                
                    # CRITICAL RULE: NO INVENTION
                    - You can ONLY use information that is explicitly present in the provided variables or user request text
                    - You MUST NOT invent, assume, generate, or hallucinate any data
                    - If information is missing to satisfy ALL required ports, return empty bindings: {}
                                
                    # INPUT SOURCES
                    You will receive:
                    - User variables in <user_variables> section (key-value pairs)
                    - Initial nodes in <nodes_list> section (with input port definitions)
                    - Optional user request in <request_text> section (natural language)
                                
                    # DATA POPULATION PROCESS
                                
                    ## Step 1: Information Extraction
                    - Extract ALL available information from user variables
                    - Extract ALL available information from user request text (if present)
                    - Create an inventory of available data points
                                
                    ## Step 2: Required Ports Analysis
                    - Identify ALL required input ports across ALL initial nodes
                    - Document what type of data each required port expects
                                
                    ## Step 3: Availability Check
                    - Verify that extracted information can satisfy EVERY required port
                    - If ANY required port cannot be populated with available data, STOP and return {}
                                
                    ## Step 4: Data Transformation (ALLOWED)
                    You MAY transform available data through:
                    - **String manipulation**: Extract substrings, split text, parse numbers from strings
                    - **Format conversion**: Convert dates, numbers, boolean representations
                    - **Unit conversion**: Convert measurements (e.g., kg to lbs, EUR to USD) ONLY if conversion rates are provided or commonly known
                    - **Data structuring**: Organize flat data into nested objects or arrays
                    - **Type casting**: Convert strings to numbers, booleans, etc.
                                
                    ## Step 5: Data Population (NOT ALLOWED)
                    You MUST NOT:
                    - Generate missing values
                    - Make assumptions about unstated information
                    - Use default values not explicitly provided
                    - Extrapolate beyond available data
                    - Create placeholder or example data
                                
                    # MAPPING SYNTAX
                    - **Simple mapping**: `"port_name": "extracted_value"`
                    - **Nested objects**: `"customer.name": "John Doe"`, `"customer.address.city": "Milano"`
                    - **Array elements**: `"items.0": "first_item"`, `"items.1": "second_item"`
                    - **Values must be primitives**: strings, numbers, booleans (never objects or arrays)
                                
                    # OUTPUT FORMAT
                    Return JSON with 'bindings' map containing ONLY successfully populated ports:
                                
                    **Success example:**
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
                                
                    **Failure example (missing required data):**
                    ```json
                    {
                      "bindings": {}
                    }
                    ```
                                
                    # VALIDATION CHECKLIST
                    Before returning bindings, verify:
                    1. Every required port across ALL nodes is populated
                    2. All values come from available user data (no invention)
                    3. All transformations are valid and lossless
                    4. All port types match expected schemas
                                
                    If ANY check fails, return empty bindings: `{"bindings": {}}`
                                
                    # EXAMPLES OF VALID TRANSFORMATIONS
                    - User variable "full_name: Mario Rossi" → `"first_name": "Mario"`, `"last_name": "Rossi"`
                    - User text "I need 5 items" → `"quantity": 5`
                    - User variable "price_eur: 100" → `"price_usd": 110` (if EUR/USD rate is known)
                    - User variables "item1: apple", "item2: banana" → `"fruits.0": "apple"`, `"fruits.1": "banana"`
                                
                    # EXAMPLES OF FORBIDDEN ACTIONS
                    - User provides "name: Mario" but port needs "email" → DON'T invent email
                    - User provides partial address but port needs complete address → DON'T fill missing parts
                    - User provides product name but port needs price → DON'T generate price
                    - Missing required data → DON'T use placeholders like "TBD" or "unknown"
                    """;
}