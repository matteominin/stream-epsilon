package org.caselli.cognitiveworkflow.operational.instances;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.LLMNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LLMPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.factories.LLMModelFactory;
import org.caselli.cognitiveworkflow.operational.LLM.PortStructuredOutputConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Component
@Scope("prototype")
public class LLMNodeInstance extends NodeInstance {

    Logger logger = LoggerFactory.getLogger(LLMNodeInstance.class);


    private ChatClient chatClient;
    private final LLMModelFactory llmModelFactory;

    public LLMNodeInstance(LLMModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    @Override
    public LLMNodeMetamodel getMetamodel() {
        return (LLMNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof LLMNodeMetamodel))
            throw new IllegalArgumentException("LLMNodeInstance requires LLMNodeMetamodel");

        super.setMetamodel(metamodel);
    }


    @Override
    public void process(ExecutionContext context) {
        logger.info("[Node {}]: Processing LLM Node Instance", getId());

        List<Message> messages = buildPromptMessages(context);
        Port responsePort = getResponsePort();

        if(responsePort == null) {
            logger.warn("[Node {}]: No response port found", getId());
            return;
        }

        try {
            // Use the helper method that handles everything
            Object result = PortStructuredOutputConverter.processWithChatClient(getChatClient(), messages, responsePort);

            // Store the result in the context
            context.put(responsePort.getKey(), result);
            logger.debug("[Node {}]: Stored result for port {} in context", getId(), responsePort.getKey());
        } catch (Exception e) {
            // Error handling
            logger.error("[Node {}]: Error processing LLM response: {}", getId(), e.getMessage(), e);
        }
    }



    private List<Message> buildPromptMessages(ExecutionContext context){
        List<Message> promptContents = new LinkedList<>();

        // SYSTEM PROMPT
        String systemPromptText = getMetamodel().getSystemPromptTemplate();
        if(systemPromptText != null && !systemPromptText.isEmpty()){
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptText);
            var model = getSystemPromptVariables(context);
            SystemMessage systemMessage = new SystemMessage(systemPromptTemplate.create(model).getContents());
            promptContents.add(systemMessage);
            logger.debug("[Node {}]: Added system prompt", getId());
        }

        // USER PROMPT
        String userInput = getUserInput(context);
        if(userInput != null && !userInput.isEmpty()){
            UserMessage userMessage = new UserMessage(userInput);
            promptContents.add(userMessage);
            logger.debug("[Node {}]: Added user prompt", getId());
        }

        // Construct prompt
        if(promptContents.isEmpty()) {
            logger.error("[Node {}]: Prompt is empty", getId());
            throw new RuntimeException("Prompt is empty");
        }
        logger.debug("[Node {}]: Built prompt messages. Count: {}", getId(), promptContents.size());
        return promptContents;
    }



    private String getUserInput (ExecutionContext context){
        List<LLMPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (LLMPort inputPort : inputPorts) {
                if (inputPort.getRole() == LLMPort.LLMPortRole.USER_PROMPT) {
                    Object value = context.get(inputPort.getKey());
                    if(value == null) {
                        logger.debug("[Node {}]: User prompt input port {} has null value in context", getId(), inputPort.getKey());
                        return null;
                    }
                    logger.debug("[Node {}]: Found user prompt input port {} with value", getId(), inputPort.getKey());
                    return value.toString();
                }
            }
        }
        logger.debug("[Node {}]: No user prompt input port found", getId());
        return null;
    }


    private Port getResponsePort (){
        List<LLMPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts != null)
            for (LLMPort inputPort : outputPorts)
                if (inputPort.getRole() == LLMPort.LLMPortRole.RESPONSE) {
                    logger.debug("[Node {}]: Found response output port {}", getId(), inputPort.getKey());
                    return inputPort;
                }
        logger.warn("[Node {}]: No response output port found", getId());
        return null;
    }



    private Map<String, Object> getSystemPromptVariables (ExecutionContext context){
        Map<String, Object> variables = new HashMap<>();

        List<LLMPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (LLMPort inputPort : inputPorts) {
                if (inputPort.getRole() == LLMPort.LLMPortRole.SYSTEM_PROMPT_VARIABLE) {
                    variables.put(inputPort.getKey(), context.get(inputPort.getKey()));
                    logger.debug("[Node {}]: Added system prompt variable {} from context", getId(), inputPort.getKey());
                }
            }
        }
        logger.debug("[Node {}]: Collected {} system prompt variables", getId(), variables.size());
        return variables;
    }



    private ChatClient getChatClient(){
        if (chatClient == null) {
            LLMNodeMetamodel metamodel = getMetamodel();
            if (metamodel == null) {
                logger.error("[Node {}]: LLMNodeInstance requires a metamodel during chat client initialization", getId());
                throw new IllegalArgumentException("LLMNodeInstance requires a metamodel");
            }
            if (metamodel.getProvider() == null) {
                logger.error("[Node {}]: initialization failed: LLM provider is not specified in the metamodel.", getId());
                throw new IllegalArgumentException("LLMNodeInstance " + getId() + " initialization failed: LLM provider is not specified in the metamodel.");
            }
            if (metamodel.getModelName() == null || metamodel.getModelName().isEmpty()) {
                logger.error("[Node {}]: initialization failed: model name is not specified in the metamodel.", getId());
                throw new IllegalArgumentException("LLMNodeInstance " + getId() + " initialization failed: model name is not specified in the metamodel.");
            }

            var config = metamodel.getDefaultLlmParameters();
            this.chatClient = llmModelFactory.createChatClient(metamodel.getProvider(), metamodel.getModelName(), null, config);

            logger.info("[Node {}]: Created ChatClient for provider {} and model {}", getId(), metamodel.getProvider(), metamodel.getModelName());
            if (config != null) logger.info("[Node {}]: LLM Parameters - Temperature: {}, TopP: {}, MaxTokens: {}", getId(), config.getTemperature(), config.getTopP(), config.getMaxTokens());
        }
        return chatClient;
    }

}