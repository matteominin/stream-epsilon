package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LlmPort;
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

    /** Parameters for the LLM call */
    private LlmModelOptions parameters;

    /** Input ports of the node */
    @NotNull private List<LlmPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<LlmPort> outputPorts = Collections.emptyList();

    public LlmNodeMetamodel() {
        super();
        this.setModelType(ModelType.LLM);
    }

    @Override
    @NotNull
    public List<LlmPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<LlmPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<LlmPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<LlmPort> outputPorts) {
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