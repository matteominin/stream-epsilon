package org.caselli.cognitiveworkflow.knowledge.model.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstract class that describes the structure and metadata of a node that can be used in a workflow.
 */
@Data
@Document(collection = "meta_nodes")
@CompoundIndex(name = "familyId_isLatest_idx", def = "{'familyId': 1, 'isLatest': 1}")
public abstract class NodeMetamodel {
    @Id
    private String id;

    /** Id of the family of all different node versions **/
    private String familyId;

    // Metadata:
    private Boolean isLatest = true; // If the node is the latest version in the current family
    @NotNull private Boolean enabled = true; // If the node is enabled
    @NotNull private Version version = new Version(0,0,0,null); // Version of the node

    @NotNull private String name;
    @NotNull private NodeType type;
    @NotNull private String description;
    @NotNull private String author;

    // Update and creation date:
    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    /** Qualitative descriptor: what the node does (non-predefined format) */
    private org.bson.Document qualitativeDescriptor;

    /** Quantitative descriptor (e.g., cSLAs, performance metrics, costs) */
    private org.bson.Document quantitativeDescriptor;


    /** Embedding field for semantic search **/
    @JsonIgnore
    private List<Double> embedding;

    // Abstract methods to be implemented by subclasses
    @NotNull public abstract List<? extends Port> getInputPorts();
    @NotNull public abstract List<? extends Port> getOutputPorts();

    public enum NodeType {
        AI,
        TOOL,FLOW
    }
}
