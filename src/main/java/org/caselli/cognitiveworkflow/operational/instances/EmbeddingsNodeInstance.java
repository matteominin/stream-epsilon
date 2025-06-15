package org.caselli.cognitiveworkflow.operational.instances;

import org.caselli.cognitiveworkflow.knowledge.model.node.EmbeddingsNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.EmbeddingsPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.factories.EmbeddingModelFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class EmbeddingsNodeInstance extends AiNodeInstance {

    private EmbeddingModel embeddingModel;
    private final EmbeddingModelFactory embeddingModelFactory;

    public EmbeddingsNodeInstance(EmbeddingModelFactory embeddingModelFactory) {
        this.embeddingModelFactory = embeddingModelFactory;
    }

    @Override
    public EmbeddingsNodeMetamodel getMetamodel() {
        return (EmbeddingsNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof EmbeddingsNodeMetamodel)) throw new IllegalArgumentException("EmbeddingsNodeInstance requires EmbeddingsNodeMetamodel");
        super.setMetamodel(metamodel);
    }


    @Override
    public void process(ExecutionContext context) {
        logger.info("[Node {}]: Processing Embedding Model Instance", getId());

        String text = getInput(context);

        if (text == null || text.trim().isEmpty()) {
            logger.error("[Node {}]: No input found", getId());
            processResultsToContext(context, null);
        } else {
            try {
                float[] embedding = getEmbeddingsModel().embed(text);
                if (embedding != null) {
                    List<Double> embeddingList = new ArrayList<>(embedding.length);
                    for (float value : embedding) embeddingList.add((double) value);
                    logger.info("[Node {}]: Completed embeddings creation", getId());
                    processResultsToContext(context, embeddingList);
                } else {
                    throw new RuntimeException("Spring AI EmbeddingModel returned null or empty output.");
                }
            } catch (Exception e) {
                logger.error("[Node {}]: Error processing Embedding Model response: {}", getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to generate embedding for text: " + text, e);
            }
        }
    }

    private void processResultsToContext(ExecutionContext context, List<Double> res) {
        var output = getResponsePort();
        if(output != null) {
            context.put(output.getKey(), res == null ? new ArrayList<>() : res);
            logger.info("[Node {}]: Exposed result vector at the port {}", getId(), output.getKey());
        }
    }

    private String getInput(ExecutionContext context){
        List<EmbeddingsPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (EmbeddingsPort inputPort : inputPorts) {
                if (inputPort.getRole() == EmbeddingsPort.EmbeddingsPortRole.INPUT_TEXT) {
                    Object value = context.get(inputPort.getKey());
                    if(value == null) {
                        logger.debug("[Node {}]: Input port {} has null value in context", getId(), inputPort.getKey());
                        continue;
                    }
                    logger.debug("[Node {}]: Input port {} with value", getId(), inputPort.getKey());
                    return value.toString();
                }
            }
        }
        return null;
    }

    private Port getResponsePort(){
        List<EmbeddingsPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts != null)
            for (EmbeddingsPort port : outputPorts)
                if (port.getRole() == EmbeddingsPort.EmbeddingsPortRole.OUTPUT_VECTOR) {
                    logger.info("[Node {}]: Found output port {}", getId(), port.getKey());
                    return port;
                }
        logger.warn("[Node {}]: No output port found", getId());
        return null;
    }


    private EmbeddingModel getEmbeddingsModel(){
        if (embeddingModel == null) {
            var metamodel = getMetamodel();

            if (metamodel == null) {
                logger.error("[Node {}]: EmbeddingsNodeInstance requires a metamodel during chat client initialization", getId());
                throw new IllegalArgumentException("LlmNodeInstance requires a metamodel");
            }
            if (metamodel.getProvider() == null) {
                logger.error("[Node {}]: Provider is not specified in the metamodel.", getId());
                throw new IllegalArgumentException("LlmNodeInstance " + getId() + " initialization failed: LLM provider is not specified in the metamodel.");
            }
            if (metamodel.getModelName() == null || metamodel.getModelName().isEmpty()) {
                logger.error("[Node {}]: Model name is not specified in the metamodel.", getId());
                throw new IllegalArgumentException("LlmNodeInstance " + getId() + " initialization failed: model name is not specified in the metamodel.");
            }

            this.embeddingModel = embeddingModelFactory.createEmbeddingModel(metamodel.getProvider(), metamodel.getModelName());
            logger.info("[Node {}]: Created EmbeddingModel for provider {} and model {}", getId(), metamodel.getProvider(), metamodel.getModelName());
        }
        return embeddingModel;
    }


    @Override
    public void handleRefreshNode(){
        // Delete the current model
        this.embeddingModel = null;
        // Build the new model
        getEmbeddingsModel();
    }
}