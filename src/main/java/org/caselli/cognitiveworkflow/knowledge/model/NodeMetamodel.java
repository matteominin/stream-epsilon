package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import org.caselli.cognitiveworkflow.knowledge.model.shared.NodeType;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Port;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "meta_nodes")
public class NodeMetamodel {
    @Id
    private String id;

    // Output / Input ports
    private List<Port> inputPorts;
    private List<Port> outputPorts;

    // Config:
    private Map<String, Object> defaultConfig;

    // Metadata:
    private String name;
    private NodeType type;
    private String description;
    private Boolean enabled;
    private Date createdAt;
    private Date updatedAt;
    private Version version;
}
