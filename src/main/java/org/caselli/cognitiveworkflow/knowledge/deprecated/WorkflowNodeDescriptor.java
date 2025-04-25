package org.caselli.cognitiveworkflow.knowledge.deprecated;

import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
public class WorkflowNodeDescriptor {
    private String id;
    private String className;
    private List<String> inputKeys;
    private List<String> outputKeys;
    private Map<String, Object> config;
    private Map<String, Object> additionalProperties;
}
