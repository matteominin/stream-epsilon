package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.shared.NodeType;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Port;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Describes the structure and metadata of a node that can be used in a workflow.
 */
@Data
@Document(collection = "meta_nodes")
public class NodeMetamodel {
    @Field("_id")
    @Id
    private String id = UUID.randomUUID().toString();


    /** Input ports definition for this node */
    private List<Port> inputPorts;

    /** Output ports definition for this node */
    private List<Port> outputPorts;


    // Config:
    private Map<String, Object> defaultConfig;

    // Metadata:
    private String name;
    private NodeType type;
    private String description;
    private Boolean enabled;
    private Version version;

    @CreatedDate private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
