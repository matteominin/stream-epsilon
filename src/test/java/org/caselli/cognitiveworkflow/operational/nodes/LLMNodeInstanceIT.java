package org.caselli.cognitiveworkflow.operational.nodes;

import org.caselli.cognitiveworkflow.knowledge.model.node.LLMNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LLMPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.LlmModelFactory;
import org.caselli.cognitiveworkflow.operational.node.LLMNodeInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@Tag("focus")
@ActiveProfiles("test")
@DisplayName("LLMNodeInstance Integration Tests")
public class LLMNodeInstanceIT {

    @Autowired
    private LlmModelFactory llmModelFactory;

    private LLMNodeInstance llmNodeInstance;
    private ExecutionContext context;
    private LLMNodeMetamodel metamodel;

    @BeforeEach
    void setUp() {
        // Initialize the LLMNodeInstance with the factory
        llmNodeInstance = new LLMNodeInstance(llmModelFactory);

        // Setup a basic metamodel for tests
        metamodel = new LLMNodeMetamodel();
        metamodel.setLlmProvider("openai"); // Assuming openai is configured and available
        metamodel.setModelName("gpt-3.5-turbo"); // Use a common test model
        llmNodeInstance.setMetamodel(metamodel);
        llmNodeInstance.setId("test-llm-node"); // Assign a test ID

        // Initialize a fresh execution context for each test
        context = new ExecutionContext();
    }


    @Test
    @DisplayName("Processing with empty prompt and system template throws exception")
    void process_withEmptyPromptAndSystemTemplate_throwsRuntimeException() {
        // Given: No input ports or system prompt template are set
        metamodel.setInputPorts(new ArrayList<>());
        metamodel.setSystemPromptTemplate(null);

        // When / Then: Processing should throw a RuntimeException (or a more specific exception if applicable)
        assertThrows(RuntimeException.class, () -> {
            llmNodeInstance.process(context);
        }, "Processing without a valid prompt should throw an exception");
    }


    @Test
    @DisplayName("Patient classification workflow returns structured string output")
    void process_patientClassificationWorkflowWithTextOutput_returnsStructuredString() {
        // Given - Setup a realistic medical classification scenario with string output schema
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
                        .withSchema(PortSchema.builder().stringSchema().build()) // Expecting string output
                        .build()
        ));

        // Realistic system prompt for a medical assistant guiding the output format
        metamodel.setSystemPromptTemplate("""
        You are an experienced medical receptionist AI. Your tasks are:
        1. Extract patient information from the input
        2. Classify the case into one of these categories: {medicalCategories}
        3. Format the response strictly as:
           [Classification] <category>
           [Patient] <name>
           [Details] <summary of symptoms>
           [Recommended Action] <suggestion>
        """);

        // Realistic test data for an emergency case
        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old.
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured response string
        String response = (String) context.get("medicalRecord");
        assertNotNull(response, "Response should not be null");
        assertFalse(response.trim().isEmpty(), "Response should not be empty");


        // Validate response structure and content using assertAll for grouped assertions
        assertAll(
                () -> assertTrue(response.contains("[Classification]"), "Response should contain [Classification] section"),
                () -> assertTrue(response.contains("EMERGENCY"), "Response classification should include EMERGENCY"),
                () -> assertTrue(response.contains("[Patient]"), "Response should contain [Patient] section"),
                () -> assertTrue(response.contains("Maria Bianchi"), "Response should contain patient name"),
                () -> assertTrue(response.contains("[Details]"), "Response should contain [Details] section"),
                () -> assertTrue(response.contains("chest pain") || response.contains("difficulty breathing"),
                        "Details should mention key symptoms (chest pain or difficulty breathing)"),
                () -> assertTrue(response.contains("[Recommended Action]"), "Response should contain [Recommended Action] section")
                // Note: Specific wording in details/action can vary by model, so check for presence of key info
        );

        // Additional validation for a different input scenario (routine checkup)
        context.put("patientInput", "Luigi Verdi here for my monthly diabetes checkup");
        llmNodeInstance.process(context); // Process again with new input

        String routineResponse = (String) context.get("medicalRecord");
        assertNotNull(routineResponse, "Response for routine checkup should not be null");
        assertTrue(routineResponse.contains("PERIODICAL_CHECK"), "Response classification should include PERIODICAL_CHECK for routine checkup");
        assertTrue(routineResponse.contains("Luigi Verdi"), "Response should contain routine checkup patient name");
    }


    @Test
    @DisplayName("Patient classification workflow returns structured map output")
    void process_patientClassificationWorkflowWithStructuredOutput_returnsMappedData() {
        // Given - Setup a realistic medical classification scenario with structured map output schema
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
                        .withSchema(PortSchema.builder().objectSchema( // Expecting a structured object (Map)
                                Map.of(
                                        "classification", PortSchema.builder().stringSchema().build(),
                                        "description", PortSchema.builder().stringSchema().build(),
                                        "user", PortSchema.builder().objectSchema(Map.of( // Nested object for user info
                                                "name",PortSchema.builder().stringSchema().build(),
                                                "surname",PortSchema.builder().stringSchema().build()
                                        )).build()
                                )
                        ).build())
                        .build()
        ));

        // System prompt guiding the LLM to produce structured output
        metamodel.setSystemPromptTemplate("""
        You are an experienced medical receptionist AI. Your tasks are:
        1. Extract patient information from the input
        2. Classify the case into one of these categories: {medicalCategories}
        3. Format the response as a JSON object strictly adhering to the provided schema.
        """);

        // Realistic test data for an emergency case
        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old.
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured map response
        Object response = context.get("medicalRecord");
        assertNotNull(response, "Response should not be null");
        assertInstanceOf(Map.class, response, "Response should be a Map (JSON object)");

        @SuppressWarnings("unchecked") // Safe cast after checking instance type
        Map<String, Object> medicalRecord = (Map<String, Object>) response;

        // Verify classification key and value
        assertTrue(medicalRecord.containsKey("classification"), "Response map should contain 'classification' key");
        assertEquals("EMERGENCY", medicalRecord.get("classification"), "Classification value should be EMERGENCY");

        // Verify description key and content
        assertTrue(medicalRecord.containsKey("description"), "Response map should contain 'description' key");
        String description = (String) medicalRecord.get("description");
        assertNotNull(description, "Description should not be null");
        assertFalse(description.trim().isEmpty(), "Description should not be empty");
        assertTrue(description.toLowerCase().contains("chest pain"), "Description should mention chest pain");
        assertTrue(description.toLowerCase().contains("difficulty breathing"), "Description should mention breathing difficulty");
        assertTrue(description.contains("150/95"), "Description should include blood pressure");

        // Verify user key and nested user information
        assertTrue(medicalRecord.containsKey("user"), "Response map should contain 'user' key");
        Object userInfoObj = medicalRecord.get("user");
        assertNotNull(userInfoObj, "'user' value should not be null");
        assertInstanceOf(Map.class, userInfoObj, "'user' value should be a Map (JSON object)");

        @SuppressWarnings("unchecked") // Safe cast after checking instance type
        Map<String, String> userInfo = (Map<String, String>) userInfoObj;

        assertTrue(userInfo.containsKey("name"), "User map should contain 'name' key");
        assertEquals("Maria", userInfo.get("name"), "User name should be Maria");

        assertTrue(userInfo.containsKey("surname"), "User map should contain 'surname' key");
        assertEquals("Bianchi", userInfo.get("surname"), "User surname should be Bianchi");
    }


    @Test
    @DisplayName("Actor filmography retrieval returns structured array of strings")
    void process_actorFilmographyRetrievalWithStructuredArrayOutput_returnsListOfStrings() {
        // Given - Setup filmography retrieval with array of strings output schema
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
                        .withSchema(PortSchema.builder().arraySchema( // Expecting an array
                                PortSchema.builder().stringSchema().build() // where each element is a string
                        ).build())
                        .build()
        ));

        // System prompt guiding the LLM to produce a JSON array of strings
        metamodel.setSystemPromptTemplate("""
        You are a film expert. When given an actor's name, respond with exactly 3
        well-known films they starred in. Format the response as a JSON array of strings.
        """);

        // Test data
        context.put("actor", "Tom Hanks");

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured list response
        Object response = context.get("filmography");
        assertNotNull(response, "Response should not be null");
        assertTrue(response instanceof List, "Response should be a list (JSON array)");

        @SuppressWarnings("unchecked")
        List<String> films = (List<String>) response;

        for (var film : films){
            System.out.println("Film: " + film);
        }

        // Verify basic response structure and content quality
        assertEquals(3, films.size(), "Should return exactly 3 films as requested");
        assertFalse(films.isEmpty(), "Film list should not be empty");
        films.forEach(film -> {
            assertNotNull(film, "Film title should not be null");
            assertFalse(film.trim().isEmpty(), "Film title should not be an empty string");
            assertTrue(film.length() >= 2, "Film title should be meaningful (at least 2 characters)");
        });

        // Verify that at least one well-known Tom Hanks film is present
        Set<String> expectedHanksFilms = Set.of(
                "Forrest Gump",
                "Cast Away",
                "The Green Mile",
                "Saving Private Ryan",
                "Toy Story",
                "Apollo 13", // Added a few more options
                "Philadelphia"
        );

        // Trim whitespace from received film titles for robust comparison
        List<String> trimmedFilms = films.stream()
                .map(String::trim)
                .collect(Collectors.toList()); // Use toList()

        assertTrue(trimmedFilms.stream().anyMatch(expectedHanksFilms::contains),
                "Should contain at least one well-known Tom Hanks film from the expected list");

        // Test with another actor
        context.put("actor", "Meryl Streep");
        llmNodeInstance.process(context);
        response = context.get("filmography");
        assertNotNull(response, "Response for Meryl Streep should not be null");
        assertTrue(response instanceof List, "Response for Meryl Streep should be a list");

        @SuppressWarnings("unchecked")
        List<String> streepFilms = (List<String>) response;
        assertEquals(3, streepFilms.size(), "Should return exactly 3 films for Meryl Streep");

        Set<String> expectedStreepFilms = Set.of(
                "The Devil Wears Prada",
                "Sophie's Choice",
                "Kramer vs Kramer",
                "Mamma Mia!",
                "The Iron Lady"
        );

        List<String> trimmedStreepFilms = streepFilms.stream().map(String::trim).toList();
        assertTrue(trimmedStreepFilms.stream().anyMatch(expectedStreepFilms::contains),
                "Should contain at least one well-known Meryl Streep film");
    }


    @Test
    @DisplayName("now 2")
    void process_actorFilmographyRetrievalWithStructuredObjectArrayOutput_returnsListOfMaps() {
        // Given - Setup filmography retrieval with array of objects output schema
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
                        .withSchema(PortSchema.builder().arraySchema( // Expecting an array
                                PortSchema.builder().objectSchema(Map.of( // where each element is an object (Map)
                                        "film_name", PortSchema.builder().stringSchema().build(),
                                        "film_year", PortSchema.builder().intSchema().build(),
                                        "film_budget", PortSchema.builder().floatSchema().build()

                                )).build()
                        ).build())
                        .build()
        ));

        // System prompt guiding the LLM to produce a JSON array of objects matching the schema
        metamodel.setSystemPromptTemplate("""
        You are a film expert. When given an actor's name, respond with exactly 3
        well-known films they starred in.
        """);

        // Test data
        context.put("actor", "Tom Hanks");

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured list of maps response
        Object response = context.get("filmography");
        assertNotNull(response, "Response should not be null");
        assertTrue(response instanceof List, "Response should be a list (JSON array)");

        @SuppressWarnings("unchecked") // Safe cast after checking instance type
        List<Map<String, Object>> films = (List<Map<String, Object>>) response;

        // Verify basic structure and content of each object in the list
        assertEquals(3, films.size(), "Should return exactly 3 film objects");
        assertFalse(films.isEmpty(), "Film list should not be empty");

        films.forEach(filmObject -> {
            assertNotNull(filmObject, "Each film object in the list should not be null");
            assertInstanceOf(Map.class, filmObject, "Each element in the list should be a Map (JSON object)");

            assertTrue(filmObject.containsKey("film_name"), "Film object should contain 'film_name' key");
            Object filmName = filmObject.get("film_name");
            assertNotNull(filmName, "'film_name' value should not be null");
            assertInstanceOf(String.class, filmName, "'film_name' value should be a String");
            assertFalse(((String) filmName).trim().isEmpty(), "'film_name' should not be an empty string");
            assertTrue(((String) filmName).length() >= 2, "'film_name' should be meaningful (at least 2 characters)");


            assertTrue(filmObject.containsKey("film_year"), "Film object should contain 'film_year' key");
            Object filmYear = filmObject.get("film_year");
            validateInt(filmYear);

            assertTrue(filmObject.containsKey("film_budget"), "Film object should contain 'film_budget' key");
            Object filmBudget = filmObject.get("film_budget");
            validateNumber(filmBudget);
        });


        // Verify that at least one object contains data for a well-known Tom Hanks film name
        Set<String> expectedHanksFilms = Set.of(
                "Forrest Gump",
                "Cast Away",
                "The Green Mile",
                "Saving Private Ryan",
                "Toy Story",
                "Apollo 13",
                "Philadelphia"
        );

        boolean foundKnownFilm = films.stream()
                .anyMatch(filmObject -> {
                    Object filmNameObj = filmObject.get("film_name");
                    if (filmNameObj instanceof String) {
                        String filmName = ((String) filmNameObj).trim();
                        return expectedHanksFilms.contains(filmName);
                    }
                    return false;
                });

        assertTrue(foundKnownFilm, "Should find at least one well-known Tom Hanks film name in the list of objects");

    }


    @Test
    @DisplayName("now")
    void process_actorFilmographyRetrievalWithStructuredObjectWithArray() {
        // Given - Setup filmography retrieval with array of objects output schema
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
                        .withSchema(PortSchema.builder().objectSchema(Map.of(
                        "name", PortSchema.builder().stringSchema().build(),
                        "date of birth", PortSchema.builder().dateSchema().build(),
                        "films", PortSchema.builder().arraySchema( // Expecting an array
                                    PortSchema.builder().objectSchema(Map.of( // where each element is an object (Map)
                                            "film_name", PortSchema.builder().stringSchema().build(),
                                            "film_year", PortSchema.builder().intSchema().build(),
                                            "film_budget", PortSchema.builder().floatSchema().build()
                                    )).build()
                                 ).build(),
                    "parents", PortSchema.builder().arraySchema(PortSchema.builder().stringSchema().build()).build()
                        )).build())
                        .build()
        ));

        // System prompt guiding the LLM to produce a JSON array of objects matching the schema
        metamodel.setSystemPromptTemplate("""
        You are a film expert. When given an actor's name, complete its profile.
        """);

        // Test data
        context.put("actor", "Tom Hanks");

        // When - Process the request
        llmNodeInstance.process(context);

        // Then - Verify the structured list of maps response
        Object response = context.get("filmography");
        assertNotNull(response, "Response should not be null");
        assertInstanceOf(Map.class, response, "Response should be a list (JSON array)");

        @SuppressWarnings("unchecked") // Safe cast after checking instance type
        Map<String, Object> profile = (Map<String, Object>) response;

        System.out.println("profile "+profile);

        // Assertions on top-level keys
        assertEquals("Tom Hanks", profile.get("name"), "Actor's name should be 'Tom Hanks'");
        assertTrue(profile.containsKey("date of birth"), "Profile should contain 'date of birth'");
        assertTrue(profile.get("date of birth") instanceof String, "'date of birth' should be a string");

        assertTrue(profile.containsKey("films"), "Profile should contain 'films'");
        Object filmsObj = profile.get("films");
        assertInstanceOf(List.class, filmsObj, "'films' should be a list");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> films = (List<Map<String, String>>) filmsObj;
        assertFalse(films.isEmpty(), "Films list should not be empty");

        // Validate structure of first film
        Map<String, String> firstFilm = films.get(0);
        assertTrue(firstFilm.containsKey("film_name"), "Each film should contain 'film_name'");
        assertTrue(firstFilm.containsKey("film_year"), "Each film should contain 'film_year'");
        assertTrue(firstFilm.containsKey("film_budget"), "Each film should contain 'film_budget'");

        // Optional: check types and values are non-empty strings
        for (Map<String, String> film : films) {
            assertNotNull(film.get("film_name"), "Film name should not be null");
            assertFalse(film.get("film_name").isEmpty(), "Film name should not be empty");
            assertNotNull(film.get("film_year"), "Film year should not be null");
            assertFalse(film.get("film_year").isEmpty(), "Film year should not be empty");
            assertNotNull(film.get("film_budget"), "Film budget should not be null");
            assertFalse(film.get("film_budget").isEmpty(), "Film budget should not be empty");
        }


        // --- Validation for 'parents' array ---
        assertInstanceOf(List.class, profile.get("parents"));
        List<?> parents = (List<?>) profile.get("parents");

        assertEquals(2, parents.size()); // Assuming 2 parents from the mock data

        for (Object parent : parents) {
            validateString(parent);
        }
    }


    private void validateString(Object obj){
        assertNotNull(obj, "String should not be null");
        assertInstanceOf(String.class, obj);
        String str = (String) obj;
        assertFalse(str.contains("\""), "string should not contain quotes");
    }

    private void validateNumber(Object obj) {
        assertNotNull(obj, "Number should not be null");
        assertInstanceOf(Number.class, obj, "Object should be a number (Integer, Float, Double, Long, etc.)");
    }

    private void validateInt(Object obj) {
        assertNotNull(obj, "Number should not be null");
        assertInstanceOf(Integer.class, obj, "Object should be an Integer");
    }
}