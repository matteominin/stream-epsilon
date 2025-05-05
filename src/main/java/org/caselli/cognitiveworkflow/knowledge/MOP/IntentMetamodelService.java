package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.validation.Valid;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.IntentMetamodelCatalog;
import org.caselli.cognitiveworkflow.operational.LLM.EmbeddingService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Validated
@Service
public class IntentMetamodelService {

    private final IntentMetamodelCatalog repository;

    private final EmbeddingService embeddingService;

    public IntentMetamodelService(IntentMetamodelCatalog repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    /**
     * Find all intents in the catalog
     * @return List of intents
     */
    @Cacheable(value = "intents")
    public List<IntentMetamodel> findAll() {
        return repository.findAll();
    }


    /**
     * Find an intent by its id
     * @param id Intent id
     * @return Optional of IntentMetamodel
     */
    @Cacheable(value = "intentModels", key = "#id")
    public Optional<IntentMetamodel> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Create a new intent in the catalog
     * @param intent Intent to create
     * @return Created IntentMetamodel
     */
    @CacheEvict(value = "intentModels", allEntries = true)
    public IntentMetamodel create(@Valid IntentMetamodel intent) {
        intent.setId(UUID.randomUUID().toString());  // Always ignore the ID provided by the user
        return repository.save(intent);
    }

    /**
     * Update an existing intent in the catalog
     * @param intent Intent to update
     * @return Updated IntentMetamodel
     */
    @CacheEvict(value = "intentModels", allEntries = true)
    public IntentMetamodel update(@Valid IntentMetamodel intent) {
        // Check it exists
        var existingIntent = repository.findById(intent.getId());
        if (existingIntent.isEmpty())
            throw new IllegalArgumentException("Intent with id " + intent.getId() + " does not exist");

        return repository.save(intent);
    }

    /**
     * Delete an intent from the catalog
     * @param id Intent id
     * @return true if the intent was deleted, false otherwise
     */
    @Cacheable(value = "intentModels", key = "#id")
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Delete an intent from the catalog
     * @param id Id of the intent to update
     */
    @CacheEvict(value = "intentModels", allEntries = true)
    public void deleteById(String id) {
        repository.deleteById(id);

        // TODO: handle deletion of intents that are referenced by workflows
    }


    /**
     * Private helper method to generate embedding for an intent
     * and set it on the intent object.
     * @param intent The intent object to generate embedding for.
     */
    private void generateAndSetEmbedding(IntentMetamodel intent) {
        String textToEmbed = intent.getName() + " " + intent.getDescription();

        // Generate the embedding using the embedding service
        List<Double> embedding = embeddingService.generateEmbedding(textToEmbed);

        // Set the generated embedding on the intent object
        intent.setEmbedding(embedding);
    }
}