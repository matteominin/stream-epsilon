package org.caselli.cognitiveworkflow.knowledge.model;
import lombok.Data;
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
    private String name;
    private String type;
    private String description;
    private List<String> inputKeys;
    private List<String> outputKeys;
    private Map<String, Object> config;
    private Boolean enabled;
    private Date createdAt;
    private Date updatedAt;
}
