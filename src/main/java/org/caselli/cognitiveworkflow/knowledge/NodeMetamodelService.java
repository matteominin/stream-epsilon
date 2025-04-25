package org.caselli.cognitiveworkflow.knowledge;

import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeMetamodelService {

    private final NodeMetamodelCatalog repository;

    public NodeMetamodelService(NodeMetamodelCatalog repository) {
        this.repository = repository;
    }

    public List<NodeMetamodel> getAllMetaNodes() {
        return repository.findAll();
    }

    public NodeMetamodel saveMetaNode(NodeMetamodel nodeMetamodel) {
        return repository.save(nodeMetamodel);
    }
}
