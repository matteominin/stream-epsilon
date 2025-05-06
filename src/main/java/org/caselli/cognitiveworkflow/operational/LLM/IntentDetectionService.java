package org.caselli.cognitiveworkflow.operational.LLM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.operational.utils.StringUtils;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intent Detector Service
 */
@Service
public class IntentDetectionService {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(IntentDetectionService.class);

    private final LlmModelFactory llmModelFactory;

    private ChatClient chatClient;

    @Value("${intent-detector.llm.provider}")
    private String intentProvider;

    @Value("${intent-detector.llm.api-key}")
    private String intentApiKey;

    @Value("${intent-detector.llm.model}")
    private String intentModel;

    @Value("${intent-detector.llm.temperature}")
    private double temperature;

    private final IntentMetamodelService intentMetamodelService;

    private static final String SYSTEM_INSTRUCTIONS_TEMPLATE =
            """
            You are a highly accurate Intent Detection System.

            Your primary goal is to identify a user's intent and provide a structured JSON output. You should only return null for truly unintelligible input.

            Available Intents:
            {availableIntents}

            ## Instructions:
            1.  **CRITICAL: Evaluate if the user input is absolutely nonsensical or unintelligible.** Consider if it's just random characters, empty, or completely lacks any form of coherent language. If it is, and you cannot extract any meaning or intent whatsoever, then and *only then* return `null`. This is an exceptional case.
            2.  **IF THE INPUT IS NOT NONSENSICAL (most cases):** Identify the core intention, goal, or request expressed in the user input.
            3.  **Compare the identified intention against the descriptions of the AVAILABLE INTENTS.** Aim for a clear and confident match to an existing intent whenever the user's request aligns well with one.
            4.  **Determine the best fit:**
                * If the user's clear intention is a strong match for one of the AVAILABLE INTENTS, select that existing intent.
                * If the user's clear intention does not have a strong match in the AVAILABLE INTENTS, but the intention is clearly discernible, PROPOSE a NEW intent name that accurately captures this distinct intention.
            5.  **Extract all relevant variables** from the user input that are necessary to understand or fulfill the identified intent (either existing or new). Format variable names as UPPERCASE_WITH_UNDERSCORES.
            6.  **Assign a FLOAT confidence score** (0.0 to 1.0). This score should reflect your certainty in the identified intent (whether existing or new) and extracted variables. Use higher scores (0.7-1.0) for strong matches to existing intents, moderate scores (0.4-0.7) for clear but new intents, and lower scores (below 0.4) might indicate a weaker or less clear intention, though ideally Step 1 handles truly unclear cases.

            ## Output Guidelines:
            - Your output MUST be valid JSON or literally the word "null".
            - **Return JSON** in the vast majority of cases where input is not nonsensical. The JSON must include:
                - "intentName": The exact Intent Name from the Available Intents list for matches, or a proposed name in UPPERCASE_WITH_UNDERSCORES for new intents.
                - "confidence": Your calculated confidence score (0.0 to 1.0).
                - "userVariables": A JSON object containing extracted variables ({{}} if none). Variable names must be UPPERCASE_WITH_UNDERSCORES.
                - "intentId": Include the exact Intent ID from the Available Intents list ONLY when matching an existing intent. OMIT this field entirely when proposing a new intent.
            - **Return `null`** only if the input is absolutely nonsensical or unintelligible as determined in Step 1.

            ## Remember:
            - If you don't find a strong match in the Available Intents, you MUST propose a new intent with a descriptive name.
            - Always prioritize matching to existing intents when appropriate.
            - Never return null for coherent, intelligible input like all common requests.
            
            **Think step by step before giving the final answer.**
            """;

    public IntentDetectionService(LlmModelFactory llmModelFactory, IntentMetamodelService intentMetamodelService) {
        this.llmModelFactory = llmModelFactory;
        this.intentMetamodelService = intentMetamodelService;
    }

    /**
     * Analyzes user input to determine intent using an LLM configured for this task.
     * @param userInput The text input from the user.
     * @return The determined intent as an IntentDetectorResult.
     *         <ul>
     *             <li>Return a predefined intent if a match is found.</li>
     *             <li>Return an invented intent if no match is found.</li>
     *             <li>Return null if intent is not clear.</li>
     *         </ul>
     */
    public IntentDetectorResult detect(String userInput) {
        logger.info("Detecting intent for user input: {}...", userInput);

        // Search top-matching intents
        var intents = intentMetamodelService.findMostSimilarIntent(userInput);

        String intentsOutput = intents.stream()
                .map(intent -> String.format("- Intent ID: %s, Intent Name: %s, Description: %s",
                        intent.getId(), intent.getName(), intent.getDescription()))
                .collect(Collectors.joining("\n"));

        // Create system prompt using template
        Map<String, Object> model = Map.of("availableIntents", intentsOutput);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_INSTRUCTIONS_TEMPLATE);
        SystemMessage systemMessage = new SystemMessage(systemPromptTemplate.create(model).getContents());

        // User message
        UserMessage userMessage = new UserMessage(userInput);

        // Construct prompt
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // TODO remove
        System.out.println("Prompt: " + prompt.getContents());

        // Call the LLM
        IntentDetectorResult result = getChatClient().prompt(prompt).call().entity(IntentDetectorResult.class);


        // Determine if the intent is non-existent
        if (result == null) {
            logger.info("LLM explicitly returned NO_CLEAR_INTENT for input: {}", userInput);
            return null;
        }



        // Post-process: determine isNew flag based on intentId presence
        boolean isNew = intents.stream()
                .noneMatch(intent -> intent.getId().equals(result.getIntentId()) || intent.getName().equals(result.getIntentName()));


        if(isNew){
            result.setIntentId(null);
            result.setNew(true);

            // Format the invented intent name to be in ALL_UPPERCASE_WITH_UNDERSCORES format
            String formattedName = StringUtils.toUppercaseSnakeCase(result.getIntentName());
            result.setIntentName(formattedName);

        } else {
            result.setNew(false);

            // Perform a final check to ensure the intentId is real
            var intent = intents.stream()
                    .filter(i -> i.getId().equals(result.getIntentId()))
                    .findFirst();

            if (intent.isEmpty()) {
                // If the intentId is not found in the list of intents
                // this means that in the previous step the intent was found by name not by id
                // so we need to set the intentId to the real intentId
                result.setIntentId(intents.stream()
                        .filter(i -> i.getName().equals(result.getIntentName()))
                        .findFirst()
                        .map(IntentMetamodel::getId)
                        .orElse(null));
            }
        }

        // Format all the user variables to be in ALL_UPPERCASE_WITH_UNDERSCORES format
        var originalVariables = result.getUserVariables();
        Map<String, Object> newVariables = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : originalVariables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String formattedKey = StringUtils.toUppercaseSnakeCase(key);
            if (!formattedKey.equals(key)) newVariables.put(formattedKey, value);
            else newVariables.put(key, value);

        }
        result.setUserVariables(newVariables);

        return result;
    }

    /**
     * Returns the ChatClient instance.
     * @return ChatClient instance
     */
    private ChatClient getChatClient() {
        if (chatClient == null) {
            var options = new LlmModelFactory.BaseLlmModelOptions();
            options.setTemperature(temperature);
            chatClient = llmModelFactory.createChatClient(intentProvider, intentApiKey, intentModel, options);
        }
        return chatClient;
    }


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static public class IntentDetectorResult {
        @JsonProperty(value = "intentId") private String intentId;
        @JsonProperty(required = true, value = "intentName") private String intentName;
        @JsonProperty(required = true, value = "confidence") private double confidence;
        @JsonProperty(required = true, value = "isNew") private boolean isNew;
        @JsonProperty(required = true, value = "userVariables") private Map<String,Object> userVariables;
    }
}