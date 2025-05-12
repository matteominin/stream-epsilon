package org.caselli.cognitiveworkflow.operational.node;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.LLMNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LLMPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.LlmModelFactory;
import org.caselli.cognitiveworkflow.operational.LLM.PortStructuredOutput;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


// TODO CLEAN UP AND COMMENTS
@Setter
@Getter
@Component
@Scope("prototype")
public class LLMNodeInstance extends NodeInstance {


    private ChatClient chatClient;
    private final LlmModelFactory llmModelFactory;

    public LLMNodeInstance(LlmModelFactory llmModelFactory) {
        this.llmModelFactory = llmModelFactory;
    }

    @Override
    public LLMNodeMetamodel getMetamodel() {
        return (LLMNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof LLMNodeMetamodel)) {
            throw new IllegalArgumentException("LLMNodeInstance requires LLMNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }


    @Override
    public void process(ExecutionContext context) {
        System.out.println("Processing LLM Node Instance: " + getId());

        List<Message> messages = buildPromptMesages(context);
        Port responsePort = getResponsePort();

        if(responsePort == null){
            return;
        }


        try {
            // Use the helper method that handles everything
            Object result = PortStructuredOutput.processWithChatClient(
                    getChatClient(), messages, responsePort);





            // Store the result in the context
            context.put(responsePort.getKey(), result);
        } catch (Exception e) {
            // Error handling
            System.err.println("Error processing LLM response: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private List<Message> buildPromptMesages(ExecutionContext context){
        List<Message> promptContents = new LinkedList<>();

        // SYSTEM PROMPT
        String systemPromptText = getMetamodel().getSystemPromptTemplate();
        if(systemPromptText != null && !systemPromptText.isEmpty()){
            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptText);
            var model = getSystemPromptVariables(context);
            SystemMessage systemMessage = new SystemMessage(systemPromptTemplate.create(model).getContents());
            promptContents.add(systemMessage);
        }

        // USER PROMPT
        String userInput = getUserInput(context);
        if(userInput != null && !userInput.isEmpty()){
            UserMessage userMessage = new UserMessage(userInput);
            promptContents.add(userMessage);
        }

        // Construct prompt
        if(promptContents.isEmpty()) throw new RuntimeException("Prompt is empty");

        return promptContents;
    }



    private String getUserInput (ExecutionContext context){
        List<LLMPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (LLMPort inputPort : inputPorts) {
                if (inputPort.getRole() == LLMPort.LLMPortRole.USER_PROMPT) {
                    Object value = context.get(inputPort.getKey());
                    if(value == null) return null;
                    return value.toString();
                }
            }
        }
        return null;
    }


    private Port getResponsePort (){
        List<LLMPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts != null)
            for (LLMPort inputPort : outputPorts)
                if (inputPort.getRole() == LLMPort.LLMPortRole.RESPONSE)
                   return inputPort;
        return null;
    }



    private Map<String, Object> getSystemPromptVariables (ExecutionContext context){
        Map<String, Object> variables = new HashMap<>();

        List<LLMPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (LLMPort inputPort : inputPorts) {
                if (inputPort.getRole() == LLMPort.LLMPortRole.SYSTEM_PROMPT_VARIABLE) {
                    variables.put(inputPort.getKey(), context.get(inputPort.getKey()));
                }
            }
        }

        return variables;
    }



    private ChatClient getChatClient(){
        if (chatClient == null) {
            LLMNodeMetamodel metamodel = getMetamodel();
            if (metamodel == null) throw new IllegalArgumentException("LLMNodeInstance requires a metamodel");
            if (metamodel.getLlmProvider() == null) throw new IllegalArgumentException("LLMNodeInstance " + getId() + " initialization failed: LLM provider is not specified in the metamodel.");
            if (metamodel.getModelName() == null || metamodel.getModelName().isEmpty()) throw new IllegalArgumentException("LLMNodeInstance " + getId() + " initialization failed: model name is not specified in the metamodel.");

            this.chatClient = llmModelFactory.createChatClient(metamodel.getLlmProvider(), metamodel.getModelName());
        }
        return chatClient;
    }

}