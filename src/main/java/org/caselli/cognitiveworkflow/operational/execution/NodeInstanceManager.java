package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.event.NodeMetamodelUpdateEvent;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.caselli.cognitiveworkflow.operational.registry.NodesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NodeInstanceManager {

    private final NodesRegistry nodesRegistry;
    private final NodeFactory nodeFactory;
    private final NodeMetamodelService nodeMetamodelService;

    private final Logger logger = LoggerFactory.getLogger(NodeInstanceManager.class);

    private final ConcurrentHashMap<String, AtomicInteger> runningNodes = new ConcurrentHashMap<>();

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
        if (existing.isPresent()) {
            // There is an instance in the registry

            // If the instance is not deprecated
            if(!existing.get().isDeprecated()) return existing.get();


            // If it is deprecated but it is in execution
            if(isRunning(existing.get().getId())) return existing.get();

            // The existing node is deprecated and it is not in execution
            this.logger.info("A node instance for bode {} was found but is deprecated: deleting it", nodeMetamodel.getId());

            // Therefore, we can safely remove it from the registry
            nodesRegistry.remove(nodeMetamodel.getId());
            // Then we can proceed as it was not found
        }

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
        Optional<NodeMetamodel> metamodel = nodeMetamodelService.getById(nodeMetamodelId);
        if(metamodel.isEmpty()) throw new RuntimeException("Node metamodel not found");

        return this.getOrCreate(metamodel.get());
    }


    /**
     * Mark a node as in execution
     * @param nodeId Node ID
     */
    public void markRunning(String nodeId) {
        runningNodes.compute(nodeId, (id, count) -> {
            if (count == null) return new AtomicInteger(1);
            count.incrementAndGet();
            return count;
        });
    }


    /**
     * Mark a node as no longer in execution
     * @param nodeId Node ID
     */
    public void markFinished(String nodeId) {
        runningNodes.computeIfPresent(nodeId, (id, count) -> {
            int newVal = count.decrementAndGet();
            if (newVal <= 0) return null;
            return count;
        });
    }

    /**
     * Check if a node in in execution
     * @param workflowId Id of the node
     * @return Returns true if there is at least one instance of the node that is being executed
     */
    public boolean isRunning(String workflowId) {
        var val = runningNodes.get(workflowId);
        return val != null && val.get() > 0;
    }


    /**
     * Listens for updates to the node metamodel
     * @param event The event containing the updated metamodel
     */
    @EventListener
    public void onMetaNodeUpdated(NodeMetamodelUpdateEvent event) {
        var id = event.metamodelId();

        // Search for the instance of the metamodel
        var instance = this.nodesRegistry.get(id);
        if(instance.isPresent()){

            this.logger.info("Operation layer received metamodel update event: updating node instance for node {}", instance.get().getId());

            // If the node is running then we cannot hot swap NOW, but we re-create the whole node LATER
            if(isRunning(id)){

                // Re-Installation
                this.logger.info("Node instance {} had a breaking change update: no hot-swap, marking it as deprecated", instance.get().getId());

                // We mark it as deprecated, when the last execution of this node finishes, it will be deleted
                // from the registry (forcing its update)
                instance.get().setDeprecated(true);

            } else{
                this.logger.info("Hot-swapping node instance {} metamodel", instance.get().getId());

                // HOT-SWAP
                // Directly update the metamodel
                instance.get().setMetamodel(event.updatedMetamodel());
                // Refresh the node configs
                instance.get().handleRefreshNode();
            }
        }
        else  this.logger.info("Operation layer received metamodel update event but no instance has the updated metamodel");

    }
}
