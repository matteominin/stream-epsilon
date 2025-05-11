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

        System.out.println("responsePortKey= "+ responsePort.getKey());


        if(responsePort.getSchema().getType() == PortType.OBJECT ){



            MapOutputConverter converter = new MapOutputConverter();
            String format = converter.getFormat();


            messages.add(new SystemMessage(format));

            messages.add(new SystemMessage("""
                            The JSON field types must follow the schema below:\n
                            """ + responsePort.getSchema().toJson() + """

                            For each key, provide only its value (e.g., a number, string, nested object, or array).
                        
                            **Important Guidelines:**
                            - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                            - `type` specifies the expected type of the value (e.g., INT, FLOAT, ARRAY, OBJECT).
                            - `items` defines the schema of elements inside an array.
                            - `properties` defines the nested structure of keys inside an object.
                            """
            ));


            Prompt prompt = new Prompt(messages);


            // TODO: remove
            System.out.println(prompt.getContents());

            String response = getChatClient().prompt(prompt).call().content();

            Map<String, Object> result = converter.convert(response);

            // TODO: remove
            System.out.println("answer "+ result);


            context.put(responsePort.getKey(), result);

        }
        else if(responsePort.getSchema().getType() == PortType.ARRAY) {

            //StructuredOutputConverter<List<Map<String, Object>>> outputConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});

            messages.add(new SystemMessage("""
                            The elements of the Array must follow the schema below:\n
                            """ + responsePort.getSchema().toJson() + """

                            For each key, provide only its value (e.g., a number, string, nested object, or array).
                        
                            **Important Guidelines:**
                            - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                            - `type` specifies the expected type of the value (e.g., INT, FLOAT, ARRAY, OBJECT).
                            - `items` defines the schema of elements inside an array.
                            - `properties` defines the nested structure of keys inside an object.
                            """
            ));




            Prompt prompt = new Prompt(messages);


            // TODO: remove
            System.out.println(prompt.getContents());

            List<?> result = getChatClient().prompt(prompt).call().entity(List.class);



            // TODO: remove
            System.out.println("answer "+ result);

            context.put(responsePort.getKey(), result);
        }
        else{


            Prompt prompt = new Prompt(messages);


            String modelAnswer = getChatClient().prompt(prompt).call().content();
            // TODO: remove
            System.out.println("answer "+ modelAnswer);

            context.put(responsePort.getKey(), modelAnswer);

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



    private static Class<?> getClassByPortType(PortType portType) {
        return switch (portType) {
            case STRING -> String.class;
            case INT -> Integer.class;
            case FLOAT -> Double.class;
            case BOOLEAN -> Boolean.class;
            case DATE -> java.util.Date.class;
            case OBJECT -> Map.class;
            case ARRAY -> java.util.List.class;
        };
    }
}