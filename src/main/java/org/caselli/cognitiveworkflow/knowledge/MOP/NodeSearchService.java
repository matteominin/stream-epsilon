package org.caselli.cognitiveworkflow.knowledge.MOP;

import org.bson.Document;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.LLM.services.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;


/**
 * Service responsible for semantic search of Nodes using MongoDB Atlas Vector Search.
 */
@Service
public class NodeSearchService {

    private static final Logger log = LoggerFactory.getLogger(NodeSearchService.class);
    private final MongoTemplate mongoTemplate;
    private final EmbeddingService embeddingService;
    private final String collectionName ="meta_nodes";
    private final String vectorSearchIndexName = "node_vector_index";  // The name of the Atlas Vector Search index
    private final String vectorSearchFieldName = "embedding"; // The embedding field name
    private final int numCandidates = 100; // Number of documents to scan per shard
    private final int limit = 10; // Number of results to return

    @Autowired
    public NodeSearchService(MongoTemplate mongoTemplate, EmbeddingService embeddingService) {
        this.mongoTemplate = mongoTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Finds the most similar NodesMetamodels based on the input text.
     * Uses MongoDB Atlas Vector Search to perform similarity search on intent embeddings.
     * @param input The text input from the user.
     * @return An Optional containing the best matching NodeMetamodel if found above the threshold,
     * otherwise an empty Optional.
     */
    public List<NodeMetamodel> performSemanticSearch(String input) {
        if (input == null || input.trim().isEmpty()) {
            log.warn("Received empty or null user input for intent search.");
            return Collections.emptyList();
        }

        try {
            // Generate embedding for the input
            List<Double> userInputEmbedding = embeddingService.generateEmbedding(input);

            if (userInputEmbedding == null || userInputEmbedding.isEmpty()) {
                log.error("Failed to generate embedding for user input.");
                return Collections.emptyList();
            }

            // Create the aggregation pipeline for vector search
            Document vectorSearch = new Document("$vectorSearch", new Document("queryVector", userInputEmbedding)
                    .append("path", vectorSearchFieldName)
                    .append("numCandidates", numCandidates)
                    .append("limit", limit)
                    .append("index", vectorSearchIndexName)
            );

            Aggregation aggregation = Aggregation.newAggregation(
                    Aggregation.stage(Document.parse(vectorSearch.toJson()))
            );

            AggregationResults<NodeMetamodel> results = mongoTemplate.aggregate(
                    aggregation,
                    collectionName,
                    NodeMetamodel.class
            );

            return results.getMappedResults();
        }
        catch (Exception e) {
            log.error("Error during intent search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
