package org.caselli.cognitiveworkflow.knowledge.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.EmbeddingsPort;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "meta_nodes")
public class EmbeddingsNodeMetamodel extends AiNodeMetamodel {

    /** Input ports of the node */
    @NotNull private List<EmbeddingsPort> inputPorts = Collections.emptyList();

    /** Output ports of the node */
    @NotNull private List<EmbeddingsPort> outputPorts = Collections.emptyList();

    public EmbeddingsNodeMetamodel() {
        super();
        this.setModelType(ModelType.EMBEDDINGS);
    }

    @Override
    @NotNull
    public List<EmbeddingsPort> getInputPorts() {
        return this.inputPorts;
    }

    @Override
    @NotNull
    public List<EmbeddingsPort> getOutputPorts() {
        return this.outputPorts;
    }

    public void setInputPorts(List<EmbeddingsPort> inputPorts) {
        // Use defensive copying
        this.inputPorts = inputPorts != null ? List.copyOf(inputPorts) : Collections.emptyList();
    }

    public void setOutputPorts(List<EmbeddingsPort> outputPorts) {
        // Use defensive copying
        this.outputPorts = outputPorts != null ? List.copyOf(outputPorts) : Collections.emptyList();
    }
}