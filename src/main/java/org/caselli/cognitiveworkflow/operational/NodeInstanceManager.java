package org.caselli.cognitiveworkflow.operational;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.core.NodeFactory;
import org.caselli.cognitiveworkflow.operational.registry.NodesRegistry;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class NodeInstanceManager {

    private final NodesRegistry nodesRegistry;
    private final NodeFactory nodeFactory;
    private final NodeMetamodelService nodeMetamodelService;

    NodeInstanceManager(
            NodesRegistry nodesRegistry,
            NodeFactory nodeFactory,
            NodeMetamodelService nodeMetamodelService
    ){
        this.nodesRegistry = nodesRegistry;
        this.nodeFactory = nodeFactory;
        this.nodeMetamodelService = nodeMetamodelService;
    }

    /**
     * Get or create a Node instance by its meta-model.
     * If it does not exist, it creates it and registers it.
     * @param nodeMetamodel The metamodel of the node
     * @return The existing or newly created NodeInstance
     */
    public NodeInstance getOrCreate(NodeMetamodel nodeMetamodel) {
        // Check if an instance already exists
        Optional<NodeInstance> existing = nodesRegistry.get(nodeMetamodel.getId());
        if (existing.isPresent()) return existing.get();

        // Create the new NodeInstance if it doesn't exist
        NodeInstance newInstance = nodeFactory.create(nodeMetamodel);

        // Register it
        nodesRegistry.register(nodeMetamodel.getId(), newInstance);

        return newInstance;
    }


    /**
     * Get or create a Node instance by its meta-model.
     * If it does not exist, it creates it and registers it.
     * @param nodeMetamodelId The id of the metamodel of the node
     * @return The existing or newly created NodeInstance
     */
    public NodeInstance getOrCreate(String nodeMetamodelId) {
        // Check if an instance already exists
        Optional<NodeInstance> existing = nodesRegistry.get(nodeMetamodelId);
        if (existing.isPresent()) return existing.get();

        // Fetch the MetaModel
        Optional<NodeMetamodel> metamodel = nodeMetamodelService.getNodeById(nodeMetamodelId);
        if(metamodel.isEmpty()) throw new RuntimeException("Node metamodel not found");

        return this.getOrCreate(metamodel.get());
    }
}
