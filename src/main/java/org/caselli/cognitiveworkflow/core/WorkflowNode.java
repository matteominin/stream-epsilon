package org.caselli.cognitiveworkflow.core;

import java.util.List;
import java.util.Map;

public interface WorkflowNode {

    String getId();
    void setId(String id);

    List<String> getInputKeys();
    void setInputKeys(List<String> inputKeys);

    List<String> getOutputKeys();
    void setOutputKeys(List<String> outputKeys);


    void process(Map<String, Object> context) throws Exception;
}