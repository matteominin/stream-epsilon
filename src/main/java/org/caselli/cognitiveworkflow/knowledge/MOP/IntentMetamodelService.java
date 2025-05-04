package org.caselli.cognitiveworkflow.knowledge.MOP;

import jakarta.validation.Valid;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.IntentMetamodelCatalog;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Validated
@Service
public class IntentMetamodelService {

    private final IntentMetamodelCatalog repository;

    public IntentMetamodelService(IntentMetamodelCatalog repository) {
        this.repository = repository;
    }

    /**
     * Find all intents in the catalog
     * @return List of intents
     */
    public List<IntentMetamodel> findAll() {
        return repository.findAll();
    }


    /**
     * Find an intent by its id
     * @param id Intent id
     * @return Optional of IntentMetamodel
     */
    public Optional<IntentMetamodel> findById(String id) {
        return repository.findById(id);
    }

    /**
     * Create a new intent in the catalog
     * @param intent Intent to create
     * @return Created IntentMetamodel
     */
    public IntentMetamodel create(@Valid IntentMetamodel intent) {
        intent.setId(UUID.randomUUID().toString());  // Always ignore the ID provided by the user
        return repository.save(intent);
    }

    /**
     * Update an existing intent in the catalog
     * @param intent Intent to update
     * @return Updated IntentMetamodel
     */
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
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Delete an intent from the catalog
     * @param id Id of the intent to update
     */
    public void deleteById(String id) {
        repository.deleteById(id);

        // TODO: handle deletion of intents that are referenced by workflows
    }
}