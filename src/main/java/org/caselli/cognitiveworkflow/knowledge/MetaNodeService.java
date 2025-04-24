package org.caselli.cognitiveworkflow.knowledge;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetaNodeService {

    private final MetaNodeRepository repository;

    public MetaNodeService(MetaNodeRepository repository) {
        this.repository = repository;
    }

    public List<MetaNode> getAllMetaNodes() {
        return repository.findAll();
    }

    public MetaNode saveMetaNode(MetaNode metaNode) {
        return repository.save(metaNode);
    }
}
