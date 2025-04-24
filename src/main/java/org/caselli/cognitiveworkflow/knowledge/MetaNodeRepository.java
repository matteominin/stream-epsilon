package org.caselli.cognitiveworkflow.knowledge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetaNodeRepository extends MongoRepository<MetaNode, String> {
}
