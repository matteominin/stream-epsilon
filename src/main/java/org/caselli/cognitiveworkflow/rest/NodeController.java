package org.caselli.cognitiveworkflow.rest;


import org.caselli.cognitiveworkflow.knowledge.*;
import org.caselli.cognitiveworkflow.operational.WorkflowEngine;
import org.caselli.cognitiveworkflow.operational.WorkflowNode;
import org.springframework.ai.document.MetadataMode;
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

    private final MetaNodeRepository metaNodeRepository;


    public NodeController(NodeMOP mop, NodeFactory factory, MetaNodeRepository metaNodeRepository) {
        this.metaNodeRepository = metaNodeRepository;
        this.mop = mop;
        this.factory = factory;
    }

    @GetMapping
    public Collection<WorkflowNodeDescriptor> listNodes() {
        return mop.getRegistry().list();
    }

    @PostMapping("/registerNode")
    public ResponseEntity<String> registerNode(@RequestBody MetaNode descriptor) {
        this.metaNodeRepository.save(descriptor);
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