package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.VectorSearchPort;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class VectorSearchNodeMetamodel extends ToolNodeMetamodel {

    /** Connection parameters for vector database */
    private Map<String, String> connectionParams;

    /** Vector database type */
    @NotNull private VectorDbType vectorDbType;

    /** Vector search configuration */
    private VectorSearchConfig searchConfig;

    public VectorSearchNodeMetamodel() {
        super();
        this.setType(NodeType.TOOL);
        this.setToolType(ToolNodeMetamodel.ToolType.VECTOR_SEARCH);
    }

    /** Input ports of the node */
    @NotNull private List<VectorSearchPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<VectorSearchPort> outputPorts = Collections.emptyList();

    @Override
    @NotNull
    public List<VectorSearchPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<VectorSearchPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<VectorSearchPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<VectorSearchPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }

    /**
     * Supported vector database types.
     */
    public enum VectorDbType {
        MONGODB_ATLAS,
        PINECONE
    }

    /**
     * Vector search configuration parameters.
     */
    @Data
    public static class VectorSearchConfig {
        /** Collection/table/index name to search in */
        @NotNull private String collectionName;

        /** Index name for vector search */
        private String indexName;

        /** Field containing vector embeddings */
        @NotNull private String vectorField;

        /** Database name */
        private String databaseName;

        /** Number of results to return */
        private Integer limit = 10;

        /** Similarity threshold (0.0-1.0) */
        private Double threshold;

        /** Embedding model provider (e.g., OpenAI, Cohere) */
        private String embeddingProvider;

        /** Embedding model name (e.g., text-embedding-3-small) */
        private String embeddingModel;

        /** Embedding dimension size */
        private Integer embeddingDimension;
    }
}