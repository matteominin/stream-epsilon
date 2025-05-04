package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.StandardPort;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;


// TODO clean up comments

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class LlmNodeMetamodel extends NodeMetamodel {

    @NotNull private String llmProvider;

    @NotNull private String modelName; // e.g., "gpt-4", "gemini-pro", "claude-3-opus", "llama3"

    private String promptTemplate; // Template string with placeholders like {{input_var}}

    // Default parameters for the LLM call (can be overridden at instance level)
    private Map<String, Object> defaultLlmParameters; // e.g., {"temperature": 0.7, "max_tokens": 500}


    /** Input ports of the node */
    @NotNull private List<StandardPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<StandardPort> outputPorts = Collections.emptyList();


    public LlmNodeMetamodel() {
        super();
        this.setType(NodeType.LLM);
    }


    @Override
    @NotNull
    public List<StandardPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<StandardPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<StandardPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<StandardPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }


    // Standard Input/Output Ports for many LLM nodes (can be customized):
    // Input: "prompt", "system_message", "history", specific template variables
    // Output: "response_text", "token_usage", "finish_reason"
    // These would be defined in the inputPorts/outputPorts list inherited from NodeMetamodel
}