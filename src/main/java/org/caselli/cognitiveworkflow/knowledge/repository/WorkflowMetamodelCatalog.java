package org.caselli.cognitiveworkflow.knowledge.repository;
import org.caselli.cognitiveworkflow.knowledge.model.WorkflowMetamodel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowMetamodelCatalog extends MongoRepository<WorkflowMetamodel, String> {

}
