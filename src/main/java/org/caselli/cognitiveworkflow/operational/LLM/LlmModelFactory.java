package org.caselli.cognitiveworkflow.operational.LLM;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;



@Service
public class LlmModelFactory {

    /**
     * Builds a configured OpenAI ChatModel instance.
     * @param apiKey The OpenAI API key
     * @param modelName The specific OpenAI model name
     * @param options Provider-specific options
     * @return A configured OpenAiChatModel instance.
     * @throws IllegalArgumentException if the API key is missing or options are of the wrong type.
     */
    public ChatModel buildOpenAiChatModel(String apiKey, String modelName, OpenAiChatOptions options) {
        if (!StringUtils.hasText(apiKey))
            throw new IllegalArgumentException("OpenAI API key is required.");

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

        if (options != null) {
            optionsBuilder
                    .model(options.getModel())
                    .temperature(options.getTemperature())
                    .topP(options.getTopP())
                    .maxTokens(options.getMaxTokens())
                    .frequencyPenalty(options.getFrequencyPenalty())
                    .presencePenalty(options.getPresencePenalty());
        }

        if (StringUtils.hasText(modelName))
            optionsBuilder.model(modelName);

        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(optionsBuilder.build())
                .build();
    }


    /**
     * Builds a configured Anthropic ChatModel instance.
     * @param apiKey The Anthropic API key
     * @param modelName The specific Anthropic model name
     * @param options Provider-specific options
     * @return A configured AnthropicChatModel instance
     * @throws IllegalArgumentException if the API key is missing or options are of the wrong type.
     */
    public ChatModel buildAnthropicChatModel(String apiKey, String modelName, AnthropicChatOptions options) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("Anthropic API key is required to build the model.");
        }

        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder();

        if (options != null) {
            optionsBuilder
                    .model(options.getModel())
                    .temperature(options.getTemperature())
                    .topP(options.getTopP())
                    .maxTokens(options.getMaxTokens())
                    .topK(options.getTopK())
                    .thinking(options.getThinking());
        }

        if (StringUtils.hasText(modelName))
            optionsBuilder.model(modelName);

        optionsBuilder.maxTokens((options != null ? options.getMaxTokens() : null) == null ? 1000 : options.getMaxTokens());

        return AnthropicChatModel.builder()
                .anthropicApi(new AnthropicApi(apiKey))
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    /**
     * Builds a generic ChatModel based on the specified provider.
     * @param provider The LLM provider ("openai" or "anthropic").
     * @param apiKey The API key for the provider. Must not be null or empty.
     * @param modelName The specific model name. Can be null or empty.
     * @param options Provider-specific options object (e.g., OpenAiChatOptions, AnthropicChatOptions), can be null.
     * @return A configured ChatModel instance.
     * @throws IllegalArgumentException if the provider is unsupported, API key is missing, or options type is incorrect.
     */
    public ChatModel buildChatModel(String provider, String apiKey, String modelName, Object options) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("LLM provider is required.");
        }

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                if (options != null && !(options instanceof OpenAiChatOptions))
                    throw new IllegalArgumentException("Options must be OpenAiChatOptions for provider 'openai'");

                yield buildOpenAiChatModel(apiKey, modelName, (OpenAiChatOptions) options);
            }
            case "anthropic" -> {
                if (options != null && !(options instanceof AnthropicChatOptions))
                    throw new IllegalArgumentException("Options must be AnthropicChatOptions for provider 'anthropic'");

                yield buildAnthropicChatModel(apiKey, modelName, (AnthropicChatOptions) options);
            }

            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }



    /**
     * Creates a ChatClient based on provider, key, model, and options.
     * @param provider The LLM provider.
     * @param apiKey The API key.
     * @param modelName The specific model name.
     * @param options Provider-specific options object.
     * @return A ChatClient instance.
     * @throws IllegalArgumentException if inputs are invalid.
     */
    public ChatClient createChatClient(String provider, String apiKey, String modelName, Object options) {
        ChatModel chatModel = buildChatModel(provider, apiKey, modelName, options);
        return ChatClient.create(chatModel);
    }

    /**
     * Creates a ChatClient based on provider, key, and model
     * @param provider The LLM provider.
     * @param apiKey The API key.
     * @param modelName The specific model name.
     * @return A ChatClient instance configured for the specific request.
     * @throws IllegalArgumentException if inputs are invalid.
     */
    public ChatClient createChatClient(String provider, String apiKey, String modelName) {
        return createChatClient(provider, apiKey, modelName, null);
    }

    /**
     * Creates a ChatClient from an already built ChatModel instance.
     * @param chatModel The ChatModel instance.
     * @return A ChatClient instance.
     * @throws IllegalArgumentException if chatModel is null.
     */
    public ChatClient createChatClient(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        return ChatClient.create(chatModel);
    }
}