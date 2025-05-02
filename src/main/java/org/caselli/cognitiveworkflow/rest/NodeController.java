package org.caselli.cognitiveworkflow.rest;

import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeMetamodelService nodeMetamodelService;

    @Autowired
    public NodeController(NodeMetamodelService nodeMetamodelService) {
        this.nodeMetamodelService = nodeMetamodelService;
    }


    /**
     * Get all node metamodels with pagination
     */
    @GetMapping
    public ResponseEntity<Page<NodeMetamodel>> getAllNodeMetamodels(Pageable pageable) {
        Page<NodeMetamodel> page = nodeMetamodelService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Create a new LLM node metamodel
     */
    @PostMapping("/llm")
    public ResponseEntity<LlmNodeMetamodel> createLlmNodeMetamodel(@Valid @RequestBody LlmNodeMetamodel llmNodeMetamodel) {
        LlmNodeMetamodel result = nodeMetamodelService.createLlmNode(llmNodeMetamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing LLM node metamodel
     */
    @PutMapping("/llm/{id}")
    public ResponseEntity<LlmNodeMetamodel> updateLlmNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody LlmNodeMetamodel llmNodeMetamodel) {

        llmNodeMetamodel.setId(id);
        LlmNodeMetamodel result = (LlmNodeMetamodel) nodeMetamodelService.updateNode(id, llmNodeMetamodel);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new REST Tool node metamodel
     */
    @PostMapping("/rest-tool")
    public ResponseEntity<RestToolNodeMetamodel> createRestToolNodeMetamodel(@Valid @RequestBody RestToolNodeMetamodel restToolNodeMetamodel) {
        RestToolNodeMetamodel result = nodeMetamodelService.createRestToolNode(restToolNodeMetamodel);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing REST Tool node metamodel
     */
    @PutMapping("/rest-tool/{id}")
    public ResponseEntity<RestToolNodeMetamodel> updateRestToolNodeMetamodel(
            @PathVariable String id,
            @Valid @RequestBody RestToolNodeMetamodel restToolNodeMetamodel) {

        restToolNodeMetamodel.setId(id);
        RestToolNodeMetamodel result = (RestToolNodeMetamodel) nodeMetamodelService.updateNode(id, restToolNodeMetamodel);
        return ResponseEntity.ok(result);
    }
}
