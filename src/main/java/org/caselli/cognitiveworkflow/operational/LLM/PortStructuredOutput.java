package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * A utility class for handling structured output from LLM responses based on port schemas.
 * This class simplifies working with Spring AI's output converters for different port types.
 */
public class PortStructuredOutput {

    /**
     * Creates a system message that instructs the LLM on how to format structured output
     * according to the provided port schema.
     *
     * @param port The port containing the schema for structured output
     * @return A system message with formatting instructions
     */
    public static Message createSchemaInstructionMessage(Port port) {
        PortSchema schema = port.getSchema();
        StringBuilder instructions = new StringBuilder();

        if (schema.getType() == PortType.OBJECT) {

            instructions.append("The output must follow the schema below:\n")
                    .append(schema.toJson())
                    .append("\n\n");


            instructions.append("""
                For each key, provide only its value (e.g., a number, string, nested object, or array).
                
                **Important Guidelines:**
                - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                - `type` specifies the expected type of the value (e.g., INT, FLOAT, DATE, ARRAY, OBJECT).
                - `items` defines the schema of elements inside an array.
                - `properties` defines the nested structure of keys inside an object.
                """);
        } else if (schema.getType() == PortType.ARRAY && schema.getItems().getType() == PortType.OBJECT) {

            instructions.append("The output must follow the schema below:\n")
                    .append(schema.toJson())
                    .append("\n\n");


            instructions.append("""
                Each element of the outer list should be a nested object for whose fields provide only its value (e.g., a number, string, nested object, or array).
                
                **Important Guidelines:**
                - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                - `type` specifies the expected type of the value (e.g., INT, FLOAT, ARRAY, OBJECT).
                - `items` defines the schema of elements inside an array.
                - `properties` defines the nested structure of keys inside an object.
                """);
        } else if (schema.getType() == PortType.ARRAY && schema.getItems().getType() == PortType.ARRAY) {

            instructions.append("The output must follow the schema below:\n")
                    .append(schema.toJson())
                    .append("\n\n");


            instructions.append("""
                Each element of the outer list should be another nested list
                
                If there is a nested object  whose fields provide only its value (e.g., a number, string, nested object, or array).
                
                **Important Guidelines:**
                - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                - `type` specifies the expected type of the value (e.g., INT, FLOAT, ARRAY, OBJECT).
                - `items` defines the schema of elements inside an array.
                - `properties` defines the nested structure of keys inside an object.
                """);
        }



/*
        else if (schema.getType() == PortType.INT) {
            instructions.append("""
                You have to return to return only an INTEGER

                If there is a nested object  whose fields provide only its value (e.g., a number, string, nested object, or array).

                **Important Guidelines:**
                - Do **not** include 'properties', 'items', or 'type' in the output; they are only present in the schema to help you understand the structure.
                - `type` specifies the expected type of the value (e.g., INT, FLOAT, ARRAY, OBJECT).
                - `items` defines the schema of elements inside an array.
                - `properties` defines the nested structure of keys inside an object.
                """);
        } */



        instructions.append("""
            Numbers (Float, Int, Double etc.) must be numeric, without any other characters. All numeric values must not contain underscores, and scientific notation is not allowed.
        """);


        return new SystemMessage(instructions.toString());
    }

    /**
     * Adds format instructions from the appropriate converter based on the port's schema type.
     *
     * @param messages The list of messages to add the format instruction to
     * @param port The port containing the schema
     */
    public static void addFormatInstructions(List<Message> messages, Port port) {
        PortSchema schema = port.getSchema();
        PortType portType = schema.getType();

        messages.add(createSchemaInstructionMessage(port));


        if (portType == PortType.OBJECT) {
            MapOutputConverter converter = new MapOutputConverter();
            messages.add(new SystemMessage(converter.getFormat()));
        } else if (portType == PortType.ARRAY) {
            if (schema.getItems().getType() == PortType.OBJECT) {
                StructuredOutputConverter<List<Map<String, Object>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            } else if (schema.getItems().getType() == PortType.ARRAY) {
                StructuredOutputConverter<List<List<?>>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            } else {
                StructuredOutputConverter<List<String>> converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});
                messages.add(new SystemMessage(converter.getFormat()));
            }
        }
        else if (portType == PortType.INT) {
            StructuredOutputConverter<Integer> converter = new BeanOutputConverter<>(Integer.class);
            messages.add(new SystemMessage(converter.getFormat()));
        }
        else if (portType == PortType.FLOAT) {
            StructuredOutputConverter<Float> converter = new BeanOutputConverter<>(Float.class);
            messages.add(new SystemMessage(converter.getFormat()));
        }



    }

    /**
     * Processes the LLM response according to the port's schema type.
     *
     * @param responseText The raw text response from the LLM
     * @param port The port containing the schema for structured output
     * @return The processed response as the appropriate Java type
     */
    public static Object processResponse(String responseText, Port port) {

        PortSchema schema = port.getSchema();
        PortType portType = schema.getType();

        if (portType == PortType.OBJECT) {
            MapOutputConverter converter = new MapOutputConverter();
            return converter.convert(responseText);
        } else if (portType == PortType.ARRAY) {
            try {
                if (schema.getItems().getType() == PortType.OBJECT) {
                    // For arrays of objects
                    StructuredOutputConverter<List<Map<String, Object>>> converter =
                            new BeanOutputConverter<>(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                    return converter.convert(responseText);
                } else if (schema.getItems().getType() == PortType.ARRAY) {
                    // For arrays of arrays
                    StructuredOutputConverter<List<List<?>>> converter =
                            new BeanOutputConverter<>(new ParameterizedTypeReference<List<List<?>>>() {});
                    return converter.convert(responseText);
                } else {
                    // For arrays of primitives TODO
                    StructuredOutputConverter<List<String>> converter =
                            new BeanOutputConverter<>(new ParameterizedTypeReference<List<String>>() {});
                    return converter.convert(responseText);
                }
            } catch (Exception e) {
                // Consider adding specific logging here to see the responseText that failed parsing
                System.err.println("Failed to parse LLM response: " + responseText); // Added for debugging
                throw new RuntimeException("Failed to parse LLM response", e);
            }
        }
        else if (portType == PortType.INT) {
            StructuredOutputConverter<Integer> converter = new BeanOutputConverter<>(Integer.class);
            return converter.convert(responseText);
        }
        else if (portType == PortType.FLOAT) {
            StructuredOutputConverter<Float> converter = new BeanOutputConverter<>(Float.class);
            return converter.convert(responseText);
        }

        else {

            return responseText;
        }
    }

    /**
     * Creates a bean output converter for a specific class based on the port schema.
     *
     * @param <T> The type to convert to
     * @param clazz The class to convert to
     * @return A bean output converter for the specified class
     */
    public static <T> BeanOutputConverter<T> createBeanConverter(Class<T> clazz) {
        return new BeanOutputConverter<>(clazz);
    }

    /**
     * Helper method to process the response directly with ChatClient
     *
     * @param chatClient The chat client to use for response processing
     * @param messages The messages to send to the chat client
     * @param port The port containing the schema
     * @return The processed response
     */
    public static Object processWithChatClient(ChatClient chatClient, List<Message> messages, Port port) {
        // Add format instructions
        addFormatInstructions(messages, port);

        // Create prompt
        Prompt prompt = new Prompt(messages);


        System.out.println("prompt: "+ prompt.getContents());

        // Process with chat client and get response content
        String responseText = chatClient.prompt(prompt).call().content();

        System.out.println("responseText: "+ responseText);

        // Process the response based on port type
        var res = processResponse(responseText, port);

        System.out.println("Converted response: "+ res);

        return res;
    }
}