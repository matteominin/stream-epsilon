package org.caselli.cognitiveworkflow.operational.LLM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import StringUtils

/**
 * Example service that uses the LLMClient to perform intent detection.
 * It configures the LLM parameters specific to its task via injected properties.
 */
@Service
public class IntentManager {

    private final LLMClient llmClient;

    // Inject configuration specific to the intent task
    @Value("${intent.llm.provider:openai}") // Default provider for intent tasks
    private String intentProvider;

    @Value("${intent.llm.api-key:}") // Specific key for intent tasks - IMPORTANT: Provide this in properties/env!
    private String intentApiKey;

    @Value("${intent.llm.model:gpt-4o-mini}") // Specific model for intent tasks
    private String intentModel;


    public IntentManager(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Analyzes user input to determine intent using an LLM configured for this task.
     * @param userInput The text input from the user.
     * @return The determined intent as a String (example return type).
     */
    public String determineIntent(String userInput) {
        // Basic validation of required configuration for this task
        if (!StringUtils.hasText(intentApiKey)) {
            System.err.println("IntentManager: API key ('intent.llm.api-key') is not configured!");
            return "error:config_missing"; // Indicate configuration error
        }

        // Construct the prompt for intent detection
        String prompt = "Analyze the following user input and determine the user's main intent.\n" +
                "User Input: " + userInput + "\n" +
                "Please provide the intent as a single word or short phrase.";

        // Use the LLMClient to send the prompt with the specific configuration for intent
        // The LLMClient will use the factory to create a client for this specific call
        try {
            String intent = llmClient.sendPrompt(intentProvider, intentApiKey, intentModel, prompt);
            // Basic post-processing
            return intent.trim();
        } catch (IllegalArgumentException e) {
            // Handle cases where the API key is missing (though we checked) or provider is unsupported
            System.err.println("IntentManager: Error determining intent - Invalid LLM parameters: " + e.getMessage());
            return "error:invalid_params";
        } catch (Exception e) {
            // Handle other potential exceptions from the LLM call (network, API errors, etc.)
            System.err.println("IntentManager: Unexpected error during intent determination: " + e.getMessage());

            return "error:llm_failed";
        }
    }


}