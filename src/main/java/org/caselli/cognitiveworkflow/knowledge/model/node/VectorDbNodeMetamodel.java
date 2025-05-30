package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.VectorDbPort;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the metamodel for a Vector Database node within the cognitive workflow.
 * This node is specialized for performing vector search operations.
 * <p>
 * Currently, this implementation serves as a proof of concept and primarily supports
 * MongoDB Atlas Vector Search.
 * </p>
 * <p>
 * TODO: Future development should include support for various vector database providers
 * by (1) potentially introducing multiple specialized subclasses of this metamodel or
 * (2) making the implementation provider-agnostic through configuration.
 * </p>
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class VectorDbNodeMetamodel extends ToolNodeMetamodel {

    /** Database name */
    @NotNull private String databaseName;

    /** Collection name to search in */
    @NotNull private String collectionName;

    /** Index name for vector search */
    @NotNull private String indexName;

    /** Field containing vector embeddings */
    @NotNull private String vectorField;

    /** Parameters for the LLM call */
    private VectorSearchConfig parameters;

    public VectorDbNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
        this.setToolType(ToolNodeMetamodel.ToolType.VECTOR_DB);
    }

    /** Input ports of the node */
    @NotNull private List<VectorDbPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<VectorDbPort> outputPorts = Collections.emptyList();

    @Override
    @NotNull
    public List<VectorDbPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<VectorDbPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<VectorDbPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<VectorDbPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }


    /**
     * Vector search configuration parameters.
     */
    @Data
    public static class VectorSearchConfig {
        /** Number of results to return */
        private Integer limit = 10;

        /** Similarity threshold (0.0-1.0) */
        private Double threshold;
    }
}