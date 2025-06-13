package org.caselli.cognitiveworkflow.knowledge.repository;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeMetamodelCatalog extends MongoRepository<NodeMetamodel, String> {


    /**
     * Find the latest version of a node family
     * @param familyId The family ID to search for
     * @return Optional containing the latest version, or empty if not found
     */
    Optional<NodeMetamodel> findByFamilyIdAndIsLatestTrue(String familyId);

    /**
     * Find all versions of a node family
     * @param familyId The family ID to search for
     * @return List of all versions in the family
     */
    List<NodeMetamodel> findByFamilyIdOrderByVersionDesc(String familyId);



}
