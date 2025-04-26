package org.caselli.cognitiveworkflow.knowledge.MOP;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NodeMetamodelService {

    private final NodeMetamodelCatalog repository;

    public NodeMetamodelService(NodeMetamodelCatalog repository) {
        this.repository = repository;
    }

    public Optional<NodeMetamodel> getNodeById(String id) {
        return repository.findById(id);
    }

    public List<NodeMetamodel> getAllNodes() {
        return repository.findAll();
    }

    public NodeMetamodel saveNode(NodeMetamodel nodeMetamodel) {
        return repository.save(nodeMetamodel);
    }


}
