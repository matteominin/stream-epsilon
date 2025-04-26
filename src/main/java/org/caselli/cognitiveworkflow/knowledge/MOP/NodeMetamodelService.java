package org.caselli.cognitiveworkflow.knowledge.MOP;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class NodeMetamodelService {

    private final ApplicationEventPublisher eventPublisher;

    private final NodeMetamodelCatalog repository;

    public NodeMetamodelService(NodeMetamodelCatalog repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get a specific Node Metamodel By Id
     * @param id Id of the metamodel
     * @return The requested node
     */
    public Optional<NodeMetamodel> getNodeById(String id) {
        return repository.findById(id);
    }

    /**
     * Save in the DB a new Node Metamodel
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    public NodeMetamodel createNode(NodeMetamodel nodeMetamodel) {
        if (nodeMetamodel.getId() != null && repository.existsById(nodeMetamodel.getId())) {
            throw new IllegalArgumentException("NodeMetamodel with id " + nodeMetamodel.getId() + " already exists.");
        }

        return repository.save(nodeMetamodel);
    }

    /**
     * Update an existing Node Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param id Id of the Node Metamodel
     * @param updatedData New Metamodel
     * @return Return the newly saved Document
     */
    public NodeMetamodel updateNode(String id, NodeMetamodel updatedData) {
        // Check if the documents exists
        NodeMetamodel existingNode = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NodeMetamodel with id " + id + " does not exist."));

        NodeMetamodel saved = repository.save(updatedData);

        // Notify the Operational Level of the modification
        eventPublisher.publishEvent(new NodeMetamodelUpdateEvent(id, saved));

        return saved;
    }


}
