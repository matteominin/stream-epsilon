package org.caselli.cognitiveworkflow.operational.nodes;

import org.caselli.cognitiveworkflow.knowledge.model.node.LLMNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LLMPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.Port;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.LlmModelFactory;
import org.caselli.cognitiveworkflow.operational.node.LLMNodeInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@Tag("focus")
@ActiveProfiles("test")
public class LLMNodeInstanceIT {
    @Autowired private LlmModelFactory llmModelFactory;

    private LLMNodeInstance llmNodeInstance;
    private ExecutionContext context;
    private LLMNodeMetamodel metamodel;

    @BeforeEach
    void setUp() {
        llmNodeInstance = new LLMNodeInstance(llmModelFactory);

        metamodel = new LLMNodeMetamodel();
        metamodel.setLlmProvider("openai");
        metamodel.setModelName("gpt-3.5-turbo");
        llmNodeInstance.setMetamodel(metamodel);
        llmNodeInstance.setId("test-llm-node");

        context = new ExecutionContext();
    }


    @Test
    void testProcessWithEmptyPromptThrowsException() {
        metamodel.setInputPorts(new ArrayList<>());
        metamodel.setSystemPromptTemplate(null);

        assertThrows(RuntimeException.class, () -> {
            llmNodeInstance.process(context);
        });
    }


    @Test
    void testPatientClassificationWorkflow() {
        // Given - Setup a realistic medical classification scenario
        metamodel.setInputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("medicalCategories")
                        .withRole(LLMPort.LLMPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build(),
                LLMPort.LLMBuilder()
                        .withKey("patientInput")
                        .withRole(LLMPort.LLMPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("medicalRecord")
                        .withRole(LLMPort.LLMPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        // Realistic system prompt for a medical assistant
        metamodel.setSystemPromptTemplate("""
        You are an experienced medical receptionist AI. Your tasks are:
        1. Extract patient information from the input
        2. Classify the case into one of these categories: {medicalCategories}
        3. Format the response as: 
           [Classification] <category>
           [Patient] <name>
           [Details] <summary of symptoms>
           [Recommended Action] <suggestion>
        """);

        // Realistic test data
        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old. 
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured response
        String response = (String) context.get("medicalRecord");
        assertNotNull(response, "Response should not be null");

        // Debug output
        System.out.println("=== Medical Record Output ===");
        System.out.println(response);

        // Validate response structure and content
        String finalResponse = response;
        assertAll(
                () -> assertTrue(finalResponse.contains("[Classification]"), "Missing classification section"),
                () -> assertTrue(finalResponse.contains("EMERGENCY"), "Should be classified as emergency"),
                () -> assertTrue(finalResponse.contains("Maria Bianchi"), "Should contain patient name"),
                () -> assertTrue(finalResponse.contains("chest pain") || finalResponse.contains("difficulty breathing"),
                        "Should mention symptoms"),
                () -> assertTrue(finalResponse.contains("[Recommended Action]"), "Missing recommended action")
        );

        // Additional validation for different input
        context.put("patientInput", "Luigi Verdi here for my monthly diabetes checkup");
        llmNodeInstance.process(context);
        response = (String) context.get("medicalRecord");
        assertTrue(response.contains("PERIODICAL_CHECK"), "Should classify routine checkup correctly");
    }



    @Test
    void testPatientClassificationWorkflow_SS() {
        // Given - Setup a realistic medical classification scenario
        metamodel.setInputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("medicalCategories")
                        .withRole(LLMPort.LLMPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build(),
                LLMPort.LLMBuilder()
                        .withKey("patientInput")
                        .withRole(LLMPort.LLMPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("medicalRecord")
                        .withRole(LLMPort.LLMPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().objectSchema(
                                Map.of(
                                        "classification", PortSchema.builder().stringSchema().build(),
                                        "description", PortSchema.builder().stringSchema().build(),
                                        "user", PortSchema.builder().objectSchema(Map.of(
                                                "name",PortSchema.builder().stringSchema().build(),
                                                "surname",PortSchema.builder().stringSchema().build()
                                        )).build()
                                )
                        ).build())
                        .build()
        ));

        // Realistic system prompt for a medical assistant
        metamodel.setSystemPromptTemplate("""
        You are an experienced medical receptionist AI. Your tasks are:
        1. Extract patient information from the input
        2. Classify the case into one of these categories: {medicalCategories}
        3. Format the response
        """);

        // Realistic test data
        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old. 
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured response
        Object response = context.get("medicalRecord");
        assertNotNull(response, "Response should not be null");

        // Debug output
        System.out.println("=== Response Output ===");
        System.out.println(response);

        assertInstanceOf(Map.class, response, "Response should be a map");

        @SuppressWarnings("unchecked")
        Map<String, Object> medicalRecord = (Map<String, Object>) response;

        // Debug output
        System.out.println("=== Medical Record Output ===");
        System.out.println(medicalRecord);
        System.out.println(medicalRecord.keySet());
        System.out.println(medicalRecord.entrySet());


        // Verify classification
        assertTrue(medicalRecord.containsKey("classification"), "Response should contain classification");
        assertEquals("EMERGENCY", medicalRecord.get("classification"), "Classification should be EMERGENCY");

        // Verify description contains key information
        assertTrue(medicalRecord.containsKey("description"), "Response should contain description");
        String description = (String) medicalRecord.get("description");
        assertTrue(description.contains("chest pain"), "Description should mention chest pain");
        assertTrue(description.contains("difficulty breathing"), "Description should mention breathing difficulty");
        assertTrue(description.contains("150/95"), "Description should include blood pressure");

        // Verify user information
        assertTrue(medicalRecord.containsKey("user"), "Response should contain user info");
        @SuppressWarnings("unchecked")
        Map<String, String> userInfo = (Map<String, String>) medicalRecord.get("user");
        assertEquals("Maria", userInfo.get("name"), "User name should be Maria");
        assertEquals("Bianchi", userInfo.get("surname"), "User surname should be Bianchi");
    }


    @Test
    void testActorFilmographyRetrieval_SS() {
        metamodel.setInputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("actor")
                        .withRole(LLMPort.LLMPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("filmography")
                        .withRole(LLMPort.LLMPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().stringSchema().build()
                        ).build())
                        .build()
        ));

        // Clear system prompt with specific instructions
        metamodel.setSystemPromptTemplate("""
    You are a film expert. When given an actor's name, respond with exactly 3 
    well-known films they starred in. Format the response as a JSON array of strings.
    """);


        context.put("actor", "Tom Hanks");
        llmNodeInstance.process(context);

        // Then - Verify the structured response
        Object response = context.get("filmography");
        assertNotNull(response, "Response should not be null");
        assertTrue(response instanceof List, "Response should be a list");

        @SuppressWarnings("unchecked")
        List<String> films = (List<String>) response;

        // Verify basic response structure
        assertEquals(3, films.size(), "Should return exactly 3 films");
        assertFalse(films.isEmpty(), "Film list should not be empty");
        assertFalse(films.contains(""), "Film entries should not be empty");

        // Verify content quality
        films.forEach(film -> {
            assertTrue(film.length() >= 2, "Film title should be meaningful");
            assertFalse(film.trim().isEmpty(), "Film title should not be blank");
        });

        // Verify known Tom Hanks films (at least one should match)
        Set<String> expectedHanksFilms = Set.of(
                "Forrest Gump",
                "Cast Away",
                "The Green Mile",
                "Saving Private Ryan",
                "Toy Story"
        );
        assertTrue(films.stream().anyMatch(expectedHanksFilms::contains),
                "Should contain at least one well-known Tom Hanks film");

        // Debug output
        System.out.println("=== Filmography Output ===");
        System.out.println("Actor: Tom Hanks");
        System.out.println("Top 3 Films:");
        films.forEach(System.out::println);
        System.out.println("=========================");
    }


    @Test
    void testActorFilmographyRetrieval_SS2() {
        metamodel.setInputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("actor")
                        .withRole(LLMPort.LLMPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LLMPort.LLMBuilder()
                        .withKey("filmography")
                        .withRole(LLMPort.LLMPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().objectSchema(Map.of(
                                        "film_name", PortSchema.builder().stringSchema().build(),
                                        "film_year", PortSchema.builder().stringSchema().build(), // TODO: test date type
                                        "film_budget", PortSchema.builder().stringSchema().build()// TODO: test number type
                                        )).build()
                        ).build())
                        .build()
        ));

        // Clear system prompt with specific instructions
        metamodel.setSystemPromptTemplate("""
    You are a film expert. When given an actor's name, respond with exactly 3 
    well-known films they starred in. Format the response as a JSON array of strings.
    """);


        context.put("actor", "Tom Hanks");
        llmNodeInstance.process(context);

        // Then - Verify the structured response
        Object response = context.get("filmography");
        assertNotNull(response, "Response should not be null");
        assertTrue(response instanceof List, "Response should be a list");



    }
}
