package org.caselli.cognitiveworkflow.operational.LLM;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class LLMClient {

    private final LlmModelFactory llmModelFactory;

    /**
     * Constructs the LLMClient service.
     * @param llmModelFactory The factory for creating LLM model instances.
     */
    @Autowired // Use constructor injection for dependencies
    public LLMClient(LlmModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    /**
     * Dynamically creates a ChatClient instance for the specific request and sends a prompt.
     * Allows specifying provider, API key, model, and options for this single interaction.
     * @param provider The LLM provider
     * @param apiKey The API key for the provider
     * @param modelName The specific model name
     * @param options Provider-specific options object
     * @param userPrompt The prompt text.
     * @return The response content (String).
     * @throws IllegalArgumentException if inputs are invalid or provider is unsupported.
     */
    public String sendPrompt(String provider, String apiKey, String modelName, Object options, String userPrompt) {
        ChatClient specificClient = llmModelFactory.createChatClient(provider, apiKey, modelName, options);
        return specificClient.prompt().user(userPrompt).call().content();
    }

    /**
     * Dynamically creates a ChatClient instance and sends a prompt (without specific options object).
     * @param provider The LLM provider. Must not be null or empty.
     * @param apiKey The API key. Must not be null or empty.
     * @param modelName The specific model name. Can be null or empty.
     * @param userPrompt The prompt text.
     * @return The response content (String).
     * @throws IllegalArgumentException if inputs are invalid or provider is unsupported.
     */
    public String sendPrompt(String provider, String apiKey, String modelName, String userPrompt) {
        return sendPrompt(provider, apiKey, modelName, null, userPrompt);
    }
}