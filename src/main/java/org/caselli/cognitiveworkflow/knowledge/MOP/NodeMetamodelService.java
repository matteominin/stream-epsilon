package org.caselli.cognitiveworkflow.knowledge.MOP;
import jakarta.validation.Valid;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.ToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.Optional;

@Service
@Validated
public class NodeMetamodelService {

    private final ApplicationEventPublisher eventPublisher;

    private final NodeMetamodelCatalog repository;

    public NodeMetamodelService(NodeMetamodelCatalog repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Check if a node metamodel exists by ID.
     */
    public boolean existsById(String id) {
        return repository.existsById(id);
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
     * Update an existing Node Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param id Id of the Node Metamodel
     * @param updatedData New Metamodel
     * @return Return the newly saved Document
     */
    public NodeMetamodel updateNode(String id, NodeMetamodel updatedData) {
        // TODO: da capire se fa anche senza specializzazione (?)

        // Check if the documents exists
        NodeMetamodel existingNode = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NodeMetamodel with id " + id + " does not exist."));

        NodeMetamodel saved = repository.save(updatedData);

        // Notify the Operational Level of the modification
        eventPublisher.publishEvent(new NodeMetamodelUpdateEvent(id, saved));

        return saved;
    }

    /**
     * Find all node metamodels with pagination.
     * @param pageable Pagination information
     */
    public Page<NodeMetamodel> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Save in the DB a new LLM Node Metamodel
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    public LlmNodeMetamodel createLlmNode(@Valid LlmNodeMetamodel nodeMetamodel) {
        nodeMetamodel.setId(null); // ignore the pre-existing ID
        nodeMetamodel.setType(NodeMetamodel.NodeType.LLM);
        return repository.save(nodeMetamodel);
    }

    /**
     * Save in the DB a new Node Metamodel of a REST TOOL
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    public RestToolNodeMetamodel createRestToolNode(@Valid RestToolNodeMetamodel nodeMetamodel) {
        nodeMetamodel.setId(null); // ignore the pre-existing ID
        // Set correct types
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(ToolNodeMetamodel.ToolType.REST);
        return repository.save(nodeMetamodel);
    }
}
