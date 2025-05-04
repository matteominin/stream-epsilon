package org.caselli.cognitiveworkflow.knowledge.model.node;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


/**
 * Describes the structure and metadata of a node that can be used in a workflow.
 */
@Data
@Document(collection = "meta_nodes")
public abstract class NodeMetamodel{
    @NotNull
    @Field("_id")
    @Id
    private String id = UUID.randomUUID().toString();

    // Metadata:
    @NotNull private Boolean enabled;
    @NotNull private Version version;

    @NotNull private String name;
    @NotNull private NodeType type;
    @NotNull private String description;
    @NotNull private String author;

    // Update and creation date:
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    /** Qualitative descriptor: what the node does (non-predefined format) */
    private JsonNode qualitativeDescriptor;

    /** Quantitative descriptor (e.g., cSLAs, performance metrics, costs) */
    private JsonNode quantitativeDescriptor;


    // Abstract methods to be implemented by subclasses
    @NotNull public abstract List<? extends Port> getInputPorts();
    @NotNull public abstract List<? extends Port> getOutputPorts();


    public enum NodeType {
        LLM,
        TOOL,
    }
}
