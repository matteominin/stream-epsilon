package org.caselli.cognitiveworkflow.knowledge.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.shared.NodeType;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Map;


// TODO clean up comments

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class LlmNodeMetamodel extends NodeMetamodel {

    @NotNull
    private String llmProvider;

    @NotNull
    private String modelName; // e.g., "gpt-4", "gemini-pro", "claude-3-opus", "llama3"

    private String promptTemplate; // Template string with placeholders like {{input_var}}

    // Default parameters for the LLM call (can be overridden at instance level)
    private Map<String, Object> defaultLlmParameters; // e.g., {"temperature": 0.7, "max_tokens": 500}



    public LlmNodeMetamodel() {
        super();
        this.setType(NodeType.LLM);
    }

    // Standard Input/Output Ports for many LLM nodes (can be customized):
    // Input: "prompt", "system_message", "history", specific template variables
    // Output: "response_text", "token_usage", "finish_reason"
    // These would be defined in the inputPorts/outputPorts list inherited from NodeMetamodel
}