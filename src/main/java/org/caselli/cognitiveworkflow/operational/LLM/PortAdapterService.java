package org.caselli.cognitiveworkflow.operational.LLM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service for managing port adaptation using LLM.
 * Maps source ports to target ports using LLM.
 */
@Service
public class PortAdapterService {

    private static final Logger logger = LoggerFactory.getLogger(PortAdapterService.class);

    private final LlmModelFactory llmModelFactory;
    private ChatClient chatClient;

    @Value("${adapter.llm.provider:}")
    private String intentProvider;

    @Value("${adapter.llm.api-key:}")
    private String intentApiKey;

    @Value("${adapter.llm.model:}")
    private String intentModel;

    private static final String SYSTEM_INSTRUCTIONS =
            """
                    You are a port adapter analysis system. Your job is to analyze source and target ports and generate attribute mappings.

                    Rules:
                    - Use direct paths (e.g. "A.B" not "A.schema.properties.B")
                    - Prioritize required attributes
                    - Return {"bindings": {}} if no mapping needed
                    - Return null if mapping impossible

                    You must respond with valid JSON in this format:
                    {
                      "bindings": {
                        "SourcePort.orderId": "TargetPort.id",
                        "SourcePort.customer.email": "TargetPort.contact.email"
                      }
                    }""";

    public PortAdapterService(LlmModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }


    /**
     * Adapts source ports to target ports using LLM
     * @param sourcePorts List of source ports
     * @param targetPorts List of target ports
     * @return PortAdaptation object containing the mapping.
     *         If no mapping is needed, it returns an empty map.
     *         If mapping is impossible, it returns null.
     */
    public PortAdaptation adaptPorts(List<Port> sourcePorts, List<Port> targetPorts) {
        logger.info("Starting port adaptation with LLM...");

        Objects.requireNonNull(sourcePorts, "Source ports list cannot be null");
        Objects.requireNonNull(targetPorts, "Target ports list cannot be null");

        if (sourcePorts.isEmpty()) {
            logger.warn("Source ports list is empty. No adaptation will be performed.");
            PortAdaptation emptyResult = new PortAdaptation();
            emptyResult.setBindings(Collections.emptyMap());
            return emptyResult;
        }
        if (targetPorts.isEmpty()) {
            logger.warn("Target ports list is empty. No adaptation will be performed.");
            PortAdaptation emptyResult = new PortAdaptation();
            emptyResult.setBindings(Collections.emptyMap());
            return emptyResult;
        }

        // Validate that all ports and their schemas in the input lists are not null
        sourcePorts.forEach(p -> {
            Objects.requireNonNull(p, "Null Port found in sourcePorts list");
            Objects.requireNonNull(p.getKey(), "Port key cannot be null for Source Port");
            Objects.requireNonNull(p.getSchema(), "Source port schema cannot be null for port with key " + p.getKey());
        });
        targetPorts.forEach(p -> {
            Objects.requireNonNull(p, "Null Port found in targetPorts list");
            Objects.requireNonNull(p.getKey(), "Port key cannot be null for Target Port");
            Objects.requireNonNull(p.getSchema(), "Target port schema cannot be null for port with key " + p.getKey());
        });

        if (!StringUtils.hasText(intentProvider) || !StringUtils.hasText(intentApiKey) || !StringUtils.hasText(intentModel))
            throw new IllegalArgumentException("LLM configuration (adapter.llm.provider, adapter.llm.api-key, adapter.llm.model) is missing or incomplete for port adaptation.");

        try {

            String sourcePortsDescription = sourcePorts.stream()
                    .map(this::portToJson)
                    .collect(Collectors.joining("\n,\n"));

            String targetPortsDescription = targetPorts.stream()
                    .map(this::portToJson)
                    .collect(Collectors.joining("\n,\n"));


            String userContent = "Source Ports:\n" + sourcePortsDescription + "\n\nTarget Ports:\n" + targetPortsDescription;

            // Log for debugging TODO: remove
            logger.info("System Instructions:\n{}", SYSTEM_INSTRUCTIONS);
            logger.info("User Content:\n{}", userContent);

            SystemMessage systemMessage = new SystemMessage(SYSTEM_INSTRUCTIONS);
            UserMessage userMessage = new UserMessage(userContent);
            Prompt prompt = new Prompt(List.of(userMessage, systemMessage));

            // Call the LLM with the prompt
            // Use structured output
            PortAdaptation adaptationResult = getChatClient().prompt(prompt).call().entity(PortAdaptation.class);

            if (adaptationResult != null && adaptationResult.getBindings() != null) {

                logger.info("LLM response mapping to PortAdaptation: " + adaptationResult);
                Map<String,String> adapterPorts = adaptationResult.getBindings();
                for (String key : adapterPorts.keySet()) logger.info(key + " -> " + adapterPorts.get(key));

                return adaptationResult;
            } else {
                if (adaptationResult == null) {
                    logger.warn("LLM response mapping to PortAdaptation failed");
                    return null;
                } else {
                    PortAdaptation emptyResult = new PortAdaptation();
                    emptyResult.setBindings(Collections.emptyMap());
                    logger.warn("No mapping needed. Returning empty result.");
                    return emptyResult;
                }
            }

        } catch (Exception e) {
            logger.error("Error during ports list adaptation with LLM structured output: " + e.getMessage(), e);
            return null;
        }
    }


    /**
     * Converts a Port object to JSON string representation.
     * @param port Port object to convert
     * @return JSON string representation of the Port object
     */
    private String portToJson(Port port) {
        ObjectMapper mapper = new ObjectMapper();
        // Exclude null fields
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            JsonNode portNode = mapper.valueToTree(port);
            // Remove "portType"
            if (portNode instanceof ObjectNode) ((ObjectNode) portNode).remove("portType");
            return mapper.writeValueAsString(portNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Port to JSON", e);
        }
    }


    /**
     * Returns the ChatClient instance.
     * @return ChatClient instance
     */
    private ChatClient getChatClient() {
        if (chatClient == null) {
            chatClient = llmModelFactory.createChatClient(intentProvider, intentApiKey, intentModel);
        }
        return chatClient;
    }
}