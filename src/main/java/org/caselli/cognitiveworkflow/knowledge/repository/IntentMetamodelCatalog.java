package org.caselli.cognitiveworkflow.knowledge.repository;

import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntentMetamodelCatalog extends MongoRepository<IntentMetamodel, String> {

}
