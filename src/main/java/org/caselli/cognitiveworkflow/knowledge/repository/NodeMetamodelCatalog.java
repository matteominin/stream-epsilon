package org.caselli.cognitiveworkflow.knowledge.repository;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeMetamodelCatalog extends MongoRepository<NodeMetamodel, String> {






}
