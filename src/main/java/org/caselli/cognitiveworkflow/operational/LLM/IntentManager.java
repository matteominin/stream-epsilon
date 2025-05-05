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

    private final LlmModelFactory llmModelFactory;

    // Inject configuration specific to the intent task
    @Value("${intent.llm.provider:openai}") // Default provider for intent tasks
    private String intentProvider;

    @Value("${intent.llm.api-key:}") // Specific key for intent tasks - IMPORTANT: Provide this in properties/env!
    private String intentApiKey;

    @Value("${intent.llm.model:gpt-4o-mini}") // Specific model for intent tasks
    private String intentModel;


    public IntentManager(LlmModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    /**
     * Analyzes user input to determine intent using an LLM configured for this task.
     * @param userInput The text input from the user.
     * @return The determined intent as a String (example return type).
     */
    public String determineIntent(String userInput) {
     return "Intent determined based on user input: " + userInput;
    }
}