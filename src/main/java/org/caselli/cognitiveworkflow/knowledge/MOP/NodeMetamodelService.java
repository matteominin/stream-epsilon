package org.caselli.cognitiveworkflow.knowledge.MOP;
import jakarta.annotation.Nonnull;
import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.node.*;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.caselli.cognitiveworkflow.knowledge.validation.NodeMetamodelValidator;
import org.caselli.cognitiveworkflow.knowledge.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NodeMetamodelService implements ApplicationListener<ApplicationReadyEvent>  {
    private static final Logger logger = LoggerFactory.getLogger(NodeMetamodelService.class);

    private final ApplicationEventPublisher eventPublisher;

    private final NodeMetamodelCatalog repository;
    private final NodeMetamodelValidator nodeMetamodelValidator;

    public NodeMetamodelService(NodeMetamodelCatalog repository, ApplicationEventPublisher eventPublisher,NodeMetamodelValidator nodeMetamodelValidator) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.nodeMetamodelValidator = nodeMetamodelValidator;
    }

    /**
     * Get a specific Node Metamodel By Id
     * @param id Id of the metamodel
     * @return The requested node
     */
    @Cacheable(value = "nodeMetamodels", key = "#id")
    public Optional<NodeMetamodel> getById(String id) {
        return repository.findById(id);
    }


    /**
     * Get the latest version of a node family
     * @param familyId The family ID
     * @return Optional containing the latest version
     */
    @Cacheable(value = "nodeMetamodels", key = "'family_' + #familyId + '_latest'")
    public Optional<NodeMetamodel> getLatestVersionByFamilyId(String familyId) {
        return repository.findByFamilyIdAndIsLatestTrue(familyId);
    }


    /**
     * Update an existing Node Metamodel
     * Updates the MongoDB document and notifies the Operational Layer
     * @param familyId Id of the Node Metamodel Family
     * @param updatedData New Metamodel
     * @return Return the newly saved Document
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel updateNode(String familyId, NodeMetamodel updatedData) {
        // Check if the documents exists
        var old = repository.findByFamilyIdAndIsLatestTrue(familyId);
        if(old.isEmpty()) throw new IllegalArgumentException("NodeMetamodel with familyId " + familyId + " does not exist.");
        var oldVersionDoc = old.get();


        // Check the the type of update
        var isBreaking = (updatedData.getVersion().getMajor() - old.get().getVersion().getMajor() ) > 0;

        if( isBreaking ){
            // If it's a breaking change:

            // 1) Clone the metamodel
            updatedData.setId(UUID.randomUUID().toString()); // Overwrite the metamodel id to make it a new document
            updatedData.setIsLatest(true); // Set it as the latest version
            updatedData.setFamilyId(oldVersionDoc.getFamilyId()); // Family ID

            // 2) Save the cloned metamodel as a new metamodel in the repository
            NodeMetamodel saved = repository.save(updatedData);


            // 3) Update the old metamodel
            oldVersionDoc.setIsLatest(false);
            repository.save(oldVersionDoc);


            return saved;
        }

        else {
            // If it is not a breaking change:

            // 1) Update the model
            updatedData.setId(oldVersionDoc.getId());
            updatedData.setFamilyId(oldVersionDoc.getFamilyId());

            updatedData.setUpdatedAt(LocalDateTime.now()); // Set the updated date
            updatedData.setIsLatest(true); // Set the node as the latest of its family



            NodeMetamodel saved = repository.save(updatedData);

            // 2) Notify the Operational Level of the modification
            eventPublisher.publishEvent(new NodeMetamodelUpdateEvent(updatedData.getId(), saved));

            return saved;

        }
    }

    /**
     * Find all node metamodels with pagination.
     * @param pageable Pagination information
     */
    @Cacheable(value = "nodeMetamodels")
    public Page<NodeMetamodel> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Save in the DB a new LLM Node Metamodel
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel createNodeMetamodel(LlmNodeMetamodel nodeMetamodel) throws BadRequestException {
        nodeMetamodel.setType(NodeMetamodel.NodeType.AI);
        nodeMetamodel.setModelType(AiNodeMetamodel.ModelType.LLM);
        // Set the correct port types
        nodeMetamodel.getInputPorts().forEach(port -> port.setPortType(Port.PortImplementationType.LLM));
        // Create
        return createBaseNodeMetamodel(nodeMetamodel);
    }


    /**
     * Save in the DB a new Embeddings Node Metamodel
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel createNodeMetamodel(EmbeddingsNodeMetamodel nodeMetamodel) throws BadRequestException {
        nodeMetamodel.setType(NodeMetamodel.NodeType.AI);
        nodeMetamodel.setModelType(AiNodeMetamodel.ModelType.EMBEDDINGS);
        // Set the correct port types
        nodeMetamodel.getInputPorts().forEach(port -> port.setPortType(Port.PortImplementationType.EMBEDDINGS));
        // Create
        return createBaseNodeMetamodel(nodeMetamodel);
    }

    /**
     * Save in the DB a new Node Metamodel of a REST TOOL
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel createNodeMetamodel(RestNodeMetamodel nodeMetamodel) throws BadRequestException {
        // Set correct types
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(ToolNodeMetamodel.ToolType.REST);
        // Set the correct port types
        nodeMetamodel.getInputPorts().forEach(port -> port.setPortType(Port.PortImplementationType.REST));
        // Create
        return createBaseNodeMetamodel(nodeMetamodel);
    }

    /**
     * Save in the DB a new Node Metamodel of a Vector Database
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel createNodeMetamodel(VectorDbNodeMetamodel nodeMetamodel) throws BadRequestException {
        // Set correct types
        nodeMetamodel.setType(NodeMetamodel.NodeType.TOOL);
        nodeMetamodel.setToolType(ToolNodeMetamodel.ToolType.VECTOR_DB);
        // Set the correct port types
        nodeMetamodel.getInputPorts().forEach(port -> port.setPortType(Port.PortImplementationType.VECTOR_DB));
        // Create
        return createBaseNodeMetamodel(nodeMetamodel);
    }


    /**
     * Save in the DB a new Gateway Node Metamodel
     * @param nodeMetamodel Metamodel to create
     * @return Returns the new Metamodel
     */
    @CacheEvict(value = "nodeMetamodels", allEntries = true)
    public NodeMetamodel createNodeMetamodel(GatewayNodeMetamodel nodeMetamodel) throws BadRequestException {
        nodeMetamodel.setType(NodeMetamodel.NodeType.FLOW);
        nodeMetamodel.setControlType(FlowNodeMetamodel.ControlType.GATEWAY);
        // Set the correct port types
        nodeMetamodel.getInputPorts().forEach(port -> port.setPortType(Port.PortImplementationType.STANDARD));
        // Create
        return createBaseNodeMetamodel(nodeMetamodel);
    }


    /**
     * Helper function to create a base Node Metamodel
     * @param nodeMetamodel Metamodel to create and save in the database
     * @return Returns the new Metamodel
     * @throws BadRequestException If the Node Metamodel is not valid
     */
    private NodeMetamodel createBaseNodeMetamodel(NodeMetamodel nodeMetamodel) throws BadRequestException {
        nodeMetamodel.setId(UUID.randomUUID().toString()); // Ignore the pre-existing ID
        nodeMetamodel.setFamilyId(UUID.randomUUID().toString()); // Ignore the pre-existing Family Id
        nodeMetamodel.setCreatedAt(LocalDateTime.now()); // Set the created date
        nodeMetamodel.setUpdatedAt(LocalDateTime.now()); // Set the updated date
        nodeMetamodel.setIsLatest(true); // Set the node as the latest of its family
        // Validate
        var res = nodeMetamodelValidator.validate(nodeMetamodel);
        if(!res.isValid()) throw new BadRequestException("NodeMetamodel is not valid: " + res.getErrors());
        // Create
        return repository.save(nodeMetamodel);
    }


    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        // Only for demo purposes. Not required in production.
        this.validateAllCatalog();
    }


    /**
     * Validates all workflow metamodels in the MongoDB repository.
     * Prints the validation results to the logs.
     */
    public void validateAllCatalog() {
        logger.info("-------------------------------------------------");
        logger.info("Starting validation of Node Metamodels on Startup...");

        List<NodeMetamodel> nodes = repository.findAll();

        int validCount = 0;
        int invalidCount = 0;
        int totalWarnings = 0;
        int totalErrors = 0;

        for (NodeMetamodel node : nodes) {

            ValidationResult result = nodeMetamodelValidator.validate(node);

            if (result.isValid()) {
                validCount++;
                logger.info("[{}/{}] Node metamodel with ID {} is valid.", validCount + invalidCount, nodes.size(), node.getId());


                int warningCount = result.getWarningCount();
                if (warningCount > 0) {
                    totalWarnings += warningCount;
                    logger.warn("Found {} warnings for valid node ID {}", warningCount, node.getId());
                }

                result.printWarnings(logger);

            } else {
                invalidCount++;

                logger.error("[{}/{}] Node metamodel with ID {} is invalid", validCount + invalidCount, nodes.size(), node.getId());

                // Count and log errors/warnings
                int errorCount = result.getErrorCount();
                int warningCount = result.getWarningCount();
                totalErrors += errorCount;
                totalWarnings += warningCount;

                logger.error("Found {} errors and {} warnings for invalid node ID {}", errorCount, warningCount, node.getId());

                result.printWarnings(logger);
                result.printErrors(logger);
            }
        }

        logger.info("Validation completed. Results:");
        logger.info(" - Total nodes processed: {}", nodes.size());
        logger.info(" - Valid nodes: {}", validCount);
        logger.info(" - Invalid nodes: {}", invalidCount);
        logger.info(" - Total warnings across all nodes: {}", totalWarnings);
        logger.info(" - Total errors across all nodes: {}", totalErrors);
        logger.info("-------------------------------------------------");
    }
}
