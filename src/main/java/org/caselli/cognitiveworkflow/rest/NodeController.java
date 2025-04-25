package org.caselli.cognitiveworkflow.rest;


import org.caselli.cognitiveworkflow.knowledge.deprecated.NodeMOP;
import org.caselli.cognitiveworkflow.knowledge.deprecated.WorkflowNodeDescriptor;
import org.caselli.cognitiveworkflow.knowledge.model.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.repository.NodeMetamodelCatalog;
import org.caselli.cognitiveworkflow.operational.core.NodeFactory;
import org.caselli.cognitiveworkflow.operational.core.WorkflowEngine;
import org.caselli.cognitiveworkflow.operational.WorkflowNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeMOP mop;
    private final NodeFactory factory;

    private final NodeMetamodelCatalog nodeMetamodelCatalog;


    public NodeController(NodeMOP mop, NodeFactory factory, NodeMetamodelCatalog nodeMetamodelCatalog) {
        this.nodeMetamodelCatalog = nodeMetamodelCatalog;
        this.mop = mop;
        this.factory = factory;
    }

    @GetMapping
    public Collection<WorkflowNodeDescriptor> listNodes() {
        return mop.getRegistry().list();
    }

    @PostMapping("/registerNode")
    public ResponseEntity<String> registerNode(@RequestBody NodeMetamodel descriptor) {
        this.nodeMetamodelCatalog.save(descriptor);
        return ResponseEntity.ok("Node registered: " + descriptor.getId());
    }


    @PatchMapping("/{id}/config")
    public ResponseEntity<String> updateConfig(@PathVariable String id,
                                               @RequestBody Map<String, Object> config) {
        mop.updateConfig(id, config);
        return ResponseEntity.ok("Node config updated: " + id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> removeNode(@PathVariable String id) {
        mop.remove(id);

        return ResponseEntity.ok("Node removed: " + id);
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, Object> context) throws Exception {
        List<WorkflowNode> nodes = new ArrayList<>();
        for (WorkflowNodeDescriptor desc : mop.getRegistry().list()) {
            nodes.add(factory.create(desc));
        }
        WorkflowEngine engine = new WorkflowEngine(nodes);
        return engine.execute(context);
    }
}