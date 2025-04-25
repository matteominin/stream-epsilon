package org.caselli.cognitiveworkflow.knowledge.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "meta_workflows")
public class WorkflowMetamodel {
    @Id
    private String id;
    private String name;
    private String type;
    private String description;
    private Boolean enabled;
    private Date createdAt;
    private Date updatedAt;
}