package org.caselli.cognitiveworkflow.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


// TODO: RIMUOVERE
// TODO: Visto che usiamo la confurazione manuale a meno che non si voglia un fallback questa classe non serve a nulla

@Configuration
public class LlmConfig {
    @Value("${llm.provider:}")
    private String defaultProvider;

    @Bean
    @Primary
    public ChatClient defaultChatClient() {
        if (defaultProvider == null || defaultProvider.isEmpty()) {
            return null;
        }
        // Use ChatClient.create() to build the client from the specific model
        return switch (defaultProvider.toLowerCase()) {
            case "anthropic" -> ChatClient.create(createAnthropicClient(null, null));
            case "openai" -> ChatClient.create(createOpenAiModel(null, null));
            default -> null;
        };
    }

    public OpenAiChatModel createOpenAiModel(String apiKey, String model) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
        if (model != null && !model.isEmpty())
            optionsBuilder.model(model);

        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .apiKey(apiKey)
                        .build())
                .defaultOptions(optionsBuilder.build())
                .build();
    }

    public AnthropicChatModel createAnthropicClient(String apiKey, String model) {
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder();

        if (model != null && !model.isEmpty())
            optionsBuilder.model(model);

        return AnthropicChatModel.builder()
                .anthropicApi(new AnthropicApi(apiKey))
                .defaultOptions(optionsBuilder.build())
                .build();
    }
}