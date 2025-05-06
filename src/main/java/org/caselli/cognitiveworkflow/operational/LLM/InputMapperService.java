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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Service for mapping variables extracted by user intent to the input ports of a workflow
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
     * Adapts the given variables to the input port format of one of the given nodes.
     * @param variables Map of variables
     * @param nodes List potential nodes
     * @return InputMapperResult object containing the mapping.
     *         If no mapping is needed, it returns an empty map.
     *         If mapping is impossible, it returns null. TODO: update description
     */
    public InputMapperResult mapInput(Map<String,Object> variables,  List<NodeMetamodel> nodes) {
        logger.info("Starting input mapping with LLM...");


        if (nodes.isEmpty()) {
            logger.warn("Nodes list is empty. No mapping will be performed.");
            return null;
        }

        String userContent = "";
        StringBuilder nodesListBuilder = new StringBuilder();

        try {
            userContent += "<available_nodes_list>\n";

            for (var node : nodes) {
                String sourcePortsDescription = node.getInputPorts().stream()
                        .map(Port::portToJson)
                        // TODO: note that for now we escape the braces in the JSON string
                        // As reported by others this is a workaround for a bug in Spring AI
                        // In fact, without escaping the braces, the template engine tries to find variables in the JSON
                        // See https://github.com/spring-projects/spring-ai/issues/2836
                        .map(s -> s.replace("{", "\\{").replace("}", "\\}"))
                        .collect(Collectors.joining(",\n    "));


                nodesListBuilder.append("- Node ID: ").append(node.getId()).append("\n");
                nodesListBuilder.append("- Node Name: ").append(node.getName()).append("\n");
                nodesListBuilder.append("- Node Description: ").append(node.getDescription()).append("\n");
                nodesListBuilder.append("  Input Ports:\n");
                nodesListBuilder.append("  ```json\n");
                nodesListBuilder.append("  [\n    ").append(sourcePortsDescription).append("\n  ]\n");
                nodesListBuilder.append("  ```\n");
                nodesListBuilder.append("\n");
            }

            userContent += nodesListBuilder.toString();
            userContent += "</available_nodes_list>\n\n";


            Prompt prompt = new Prompt(List.of(new SystemMessage(SYSTEM_INSTRUCTIONS), new UserMessage(userContent)));


            // Call the LLM
            // Call the LLM
            InputMapperLLMResult adaptationResult = getChatClient().prompt(prompt).call().entity(InputMapperLLMResult.class);


            if(adaptationResult == null){
                logger.error("Error during input mapping. LLM returned null");
                return null;
            }

            if(adaptationResult.getSelectedStartingNodeId() == null || adaptationResult.getSelectedStartingNodeId().isEmpty()) {
                // LLM did not identify a suitable starting node
                logger.warn("LLM did not return a selectedStartingNodeId. No suitable node found or mapping impossible.");
                return null;
            }


            // Search the complete metamodel object
            String selectedNodeId = adaptationResult.getSelectedStartingNodeId();
            Map<String, String> bindings = adaptationResult.getBindings();

            NodeMetamodel startingNode = nodes.stream()
                    .filter(node -> node.getId().equals(selectedNodeId))
                    .findFirst()
                    .orElse(null);

            if (startingNode == null) {
                logger.error("LLM selected node ID {} not found in the available nodes list.", selectedNodeId);
                return null;
            }

            ExecutionContext context = new ExecutionContext();

            if (bindings != null) {
                for (Map.Entry<String, String> binding : bindings.entrySet()) {
                    String nodeInputPath = binding.getKey();
                    String value = binding.getValue();
                    // put with dot notation
                    context.put(nodeInputPath, value);
                }
            }

            return new InputMapperResult(startingNode, context);

        } catch (Exception e) {
            logger.error("Error during input mapping: " + e.getMessage(), e);
            return null;
        }

    }

    /**
     * Returns the ChatClient instance.
     * @return ChatClient instance
     */
    private ChatClient getChatClient() {
        if (chatClient == null) {

            if (!StringUtils.hasText(intentProvider) || !StringUtils.hasText(intentApiKey) || !StringUtils.hasText(intentModel))
                throw new IllegalArgumentException("LLM configuration (adapter.llm.provider, adapter.llm.api-key, adapter.llm.model) is missing or incomplete for port adaptation.");


            var options = new LlmModelFactory.BaseLlmModelOptions();
            options.setTemperature(temperature);
            chatClient = llmModelFactory.createChatClient(intentProvider, intentApiKey, intentModel, options);
        }
        return chatClient;
    }




    @Data
    private static class InputMapperLLMResult {
        @JsonProperty(required = true)
        String selectedStartingNodeId;

        @JsonProperty(required = true)
        Map<String, String> bindings;
    }

    @Data
    @AllArgsConstructor
    public static class InputMapperResult {
        NodeMetamodel startingNode;
        ExecutionContext context;
    }

    private static final String SYSTEM_INSTRUCTIONS =
            """
            You are an accurate and reliable Input Mapper System for a workflow orchestrator.
    
            Your primary task is to receive a set of user-provided variables and a list of available workflow nodes,
            identify the most suitable starting node whose required input ports can be fully satisfied by the user variables,
            and generate a mapping that shows how the user variables should be mapped to that node's input ports.
    
            ## User Input:
            The user will provide two things:
            1. A set of unstructured user variables.
            2. A list of available workflow nodes, including descriptions and definitions of their required and optional input ports.
    
            ## STEP-BY-STEP INSTRUCTIONS:
    
            1.  **Understand User Variables:** Carefully examine the provided user variables and their structure.
            2.  **Analyze Available Nodes:** Review the list of available nodes. For each node, understand its purpose, description, and the definition of its input ports, particularly identifying which ports are *required*.
            3.  **Match Variables to Node Inputs:** For each available node, attempt to match the user variables to its required input ports. Use the node name, description, and port descriptions as context to identify potential matches, even if variable names or structures differ. Pay close attention to nested structures.
            4.  **Identify Satisfiable Nodes:** Determine which nodes have *all* of their *required* input ports that can be satisfied by the available user variables.
            5.  **Select the Initial Node:** From the list of satisfiable nodes (Step 4), select the most appropriate one to be the initial node of the workflow. (Assume there is one clear initial node that fits the criteria unless otherwise specified by the user input).
            6.  **Generate Input Map:** Create a new map according to the input port structure with dot notation. Fomat Map<String, Object>  = { path : variable value from the input do not invent }
    
            ## Output Format Rules for Mapping:
            - Use direct dot notation for paths (e.g., "user_data.address.city" mapping to "node_input.billing_city"). Avoid unnecessary structural descriptors like ".schema.properties".
            - Prioritize required attributes
    
            """;

}