package org.caselli.cognitiveworkflow.operational.LLM.factories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmbeddingModelFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates an EmbeddingModel based on the specified provider.
     * @param provider The provider name (e.g., "openai", "vertex").
     * @param apiKey The API key for the provider.
     * @return An instance of EmbeddingModel.
     */
    public EmbeddingModel createEmbeddingModel(String provider, String apiKey) {

        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAiEmbeddingModel(apiKey);
            case "vertex" -> createVertexEmbeddingModel(apiKey);
            default -> throw new IllegalStateException("Unexpected value: " + provider.toLowerCase());
        };
    }

    private EmbeddingModel createOpenAiEmbeddingModel(String apiKey) {

        if (!StringUtils.hasText(apiKey)) throw new IllegalArgumentException("OpenAI API key is required");

        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey).build();

        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(openAiApi);

        logger.info("Created OpenAI Embedding Model.");

        return openAiEmbeddingModel;
    }

    private EmbeddingModel createVertexEmbeddingModel(String apiKey) {
        if (!StringUtils.hasText(apiKey)) throw new IllegalArgumentException("OpenAI API key is required");

        // TODO: Implement Vertex Embedding Model creation
        throw new UnsupportedOperationException("Vertex Embedding Model is not yet implemented.");
    }
}