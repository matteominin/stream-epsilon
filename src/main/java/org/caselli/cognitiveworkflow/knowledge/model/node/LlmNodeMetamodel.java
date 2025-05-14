package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LLMPort;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class LlmNodeMetamodel extends AiNodeMetamodel {

    /**
     * System prompt template string with placeholders (like {{input_var}})
     */
    private String systemPromptTemplate;

    /** Default parameters for the LLM call (can be overridden at instance level TODO) */
    private LlmModelOptions defaultLlmParameters;


    /** Input ports of the node */
    @NotNull private List<LLMPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<LLMPort> outputPorts = Collections.emptyList();

    public LlmNodeMetamodel() {
        super();
        this.setModelType(ModelType.LLM);
    }

    @Override
    @NotNull
    public List<LLMPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<LLMPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<LLMPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<LLMPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }


    /**
     * A generic (non-provider specific) options class for LLM models.
     */
    @Getter
    @Setter
    public static class LlmModelOptions {
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
    }
}