package org.caselli.cognitiveworkflow.operational.instances;

import org.caselli.cognitiveworkflow.knowledge.model.node.LlmNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.LlmPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.AI.factories.LLMModelFactory;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@ActiveProfiles("test")

public class LlmNodeInstanceIT {

    @Autowired
    private LLMModelFactory llmModelFactory;

    private LlmNodeInstance llmNodeInstance;
    private ExecutionContext context;
    private LlmNodeMetamodel metamodel;

    @BeforeEach
    void setUp() {
        llmNodeInstance = new LlmNodeInstance(llmModelFactory);
        metamodel = new LlmNodeMetamodel();
        metamodel.setProvider("openai");
        metamodel.setModelName("gpt-4o");

        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(0.4);
        options.setMaxTokens(400);
        metamodel.setParameters(options);

        llmNodeInstance.setMetamodel(metamodel);
        llmNodeInstance.setId("test-llm-node");
        context = new ExecutionContext();
    }

    @Test
    @DisplayName("Empty input throws exception")
    void test_withEmptyPromptAndSystemTemplate_throwsRuntimeException() {
        metamodel.setInputPorts(new ArrayList<>());
        metamodel.setSystemPromptTemplate(null);
        assertThrows(RuntimeException.class, () -> llmNodeInstance.process(context));
    }

    @Test
    @DisplayName("Simple String output")
    void test_WithTextOutput_returnsString() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("object")
                        .withRole(LlmPort.LlmPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("color")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate("""
        You are an experienced object-detector. What is the color of this object: {object} Give the color in uppercase.>
        """);

        context.put("object", "the sky");

        llmNodeInstance.process(context);

        String response = (String) context.get("color");
        assertNotNull(response);
        assertFalse(response.trim().isEmpty());
        assertEquals("BLUE", response);
    }


    @Test
    @DisplayName("Simple Int output")
    void test_WithNumberOutput_returnsInt() {

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("res")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().intSchema().build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate("You are an wizard. Give me a random prediction of the temperature of my room");

        llmNodeInstance.process(context);

        Integer response = (Integer) context.get("res");
        validateInt(response);
    }


    @Test
    @DisplayName("Simple Int output")
    void test_WithNumberOutput_returnsFloat() {
        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("res")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().floatSchema().build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate("You are an wizard. Give me a random prediction of the temperature of my room");

        llmNodeInstance.process(context);

        Object response = context.get("res");
        validateFloat(response);
    }


    @Test
    @DisplayName("Complex string output")
    void test_patientClassificationWorkflowWithTextOutput_returnsStructuredString() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("medicalCategories")
                        .withRole(LlmPort.LlmPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build(),
                LlmPort.builder()
                        .withKey("patientInput")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("medicalRecord")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

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

        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old.
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        llmNodeInstance.process(context);

        String response = (String) context.get("medicalRecord");
        assertNotNull(response);
        assertFalse(response.trim().isEmpty());

        assertAll(
                () -> assertTrue(response.contains("[Classification]")),
                () -> assertTrue(response.contains("EMERGENCY")),
                () -> assertTrue(response.contains("[Patient]")),
                () -> assertTrue(response.contains("Maria Bianchi")),
                () -> assertTrue(response.contains("[Details]")),
                () -> assertTrue(response.contains("chest pain") || response.contains("difficulty breathing")),
                () -> assertTrue(response.contains("[Recommended Action]"))
        );

        context.put("patientInput", "Luigi Verdi here for my monthly diabetes checkup");
        llmNodeInstance.process(context);
        String routineResponse = (String) context.get("medicalRecord");
        assertNotNull(routineResponse);
        assertTrue(routineResponse.contains("PERIODICAL_CHECK"));
        assertTrue(routineResponse.contains("Luigi Verdi"));
    }

    @Test
    @DisplayName("Nested map output")
    void test_patientClassificationWorkflowWithStructuredOutput_returnsMappedData() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("medicalCategories")
                        .withRole(LlmPort.LlmPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build(),
                LlmPort.builder()
                        .withKey("patientInput")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("medicalRecord")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
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

        metamodel.setSystemPromptTemplate("""
        You are an experienced medical receptionist AI. Your tasks are:
        1. Extract patient information from the input
        2. Classify the case into one of these categories: {medicalCategories}
        3. Format the response as a JSON object strictly adhering to the provided schema.
        """);

        context.put("medicalCategories", "VISIT; EMERGENCY; PERIODICAL_CHECK; INJURY; ILLNESS");
        context.put("patientInput", """
        Emergency! This is Maria Bianchi, 45 years old.
        She has severe chest pain and difficulty breathing since 20 minutes ago.
        Blood pressure is 150/95.
        """);

        llmNodeInstance.process(context);

        Object response = context.get("medicalRecord");
        assertNotNull(response);
        assertInstanceOf(Map.class, response);

        @SuppressWarnings("unchecked")
        Map<String, Object> medicalRecord = (Map<String, Object>) response;

        assertTrue(medicalRecord.containsKey("classification"));
        assertEquals("EMERGENCY", medicalRecord.get("classification"));

        assertTrue(medicalRecord.containsKey("description"));
        String description = (String) medicalRecord.get("description");
        assertNotNull(description);
        assertFalse(description.trim().isEmpty());
        assertTrue(description.toLowerCase().contains("chest pain"));
        assertTrue(description.toLowerCase().contains("difficulty breathing"));
        assertTrue(description.contains("150/95"));

        assertTrue(medicalRecord.containsKey("user"));
        Object userInfoObj = medicalRecord.get("user");
        assertNotNull(userInfoObj);
        assertInstanceOf(Map.class, userInfoObj);

        @SuppressWarnings("unchecked")
        Map<String, String> userInfo = (Map<String, String>) userInfoObj;

        assertTrue(userInfo.containsKey("name"));
        assertEquals("Maria", userInfo.get("name"));

        assertTrue(userInfo.containsKey("surname"));
        assertEquals("Bianchi", userInfo.get("surname"));
    }

    @Test
    @DisplayName("List of strings output")
    void test_actorFilmographyRetrievalWithStructuredArrayOutput_returnsListOfStrings() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("actor")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("filmography")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().stringSchema().build()
                        ).build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate(
        """
        You are a film expert. When given an actor's name, respond with exactly 3
        well-known films they starred in. Format the response as a JSON array of strings.
        """);

        context.put("actor", "Tom Hanks");

        llmNodeInstance.process(context);

        Object response = context.get("filmography");
        assertNotNull(response);
        assertInstanceOf(List.class, response);

        @SuppressWarnings("unchecked")
        List<String> films = (List<String>) response;

        assertEquals(3, films.size());
        assertFalse(films.isEmpty());
        films.forEach(film -> {
            assertNotNull(film);
            assertFalse(film.trim().isEmpty());
            assertTrue(film.length() >= 2);
        });

        Set<String> expectedHanksFilms = Set.of(
                "Forrest Gump",
                "Cast Away",
                "The Green Mile",
                "Saving Private Ryan",
                "Toy Story",
                "Apollo 13",
                "Philadelphia"
        );

        List<String> trimmedFilms = films.stream()
                .map(String::trim)
                .toList();

        assertTrue(trimmedFilms.stream().anyMatch(expectedHanksFilms::contains));

        context.put("actor", "Meryl Streep");
        llmNodeInstance.process(context);
        response = context.get("filmography");
        assertNotNull(response);
        assertInstanceOf(List.class, response);

        @SuppressWarnings("unchecked")
        List<String> streepFilms = (List<String>) response;
        assertEquals(3, streepFilms.size());

        Set<String> expectedStreepFilms = Set.of(
                "The Devil Wears Prada",
                "Sophie's Choice",
                "Kramer vs Kramer",
                "Mamma Mia!",
                "The Iron Lady"
        );

        List<String> trimmedStreepFilms = streepFilms.stream().map(String::trim).toList();
        assertTrue(trimmedStreepFilms.stream().anyMatch(expectedStreepFilms::contains));
    }

    @Test
    @DisplayName("List of maps output")
    void test_actorFilmographyRetrievalWithStructuredObjectArrayOutput_returnsListOfMaps() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("actor")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("filmography")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().objectSchema(Map.of(
                                        "film_name", PortSchema.builder().stringSchema().build(),
                                        "film_year", PortSchema.builder().intSchema().build(),
                                        "film_budget", PortSchema.builder().floatSchema().build()
                                )).build()
                        ).build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate("""
        You are a film expert. When given an actor's name, respond with exactly 3
        well-known films they starred in.
        """);

        context.put("actor", "Tom Hanks");

        llmNodeInstance.process(context);

        Object response = context.get("filmography");
        assertNotNull(response);
        assertInstanceOf(List.class, response);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> films = (List<Map<String, Object>>) response;

        assertEquals(3, films.size());
        assertFalse(films.isEmpty());

        films.forEach(filmObject -> {
            assertNotNull(filmObject);
            assertInstanceOf(Map.class, filmObject);

            assertTrue(filmObject.containsKey("film_name"));
            Object filmName = filmObject.get("film_name");
            assertNotNull(filmName);
            assertInstanceOf(String.class, filmName);
            assertFalse(((String) filmName).trim().isEmpty());
            assertTrue(((String) filmName).length() >= 2);

            assertTrue(filmObject.containsKey("film_year"));
            Object filmYear = filmObject.get("film_year");
            validateInt(filmYear);

            assertTrue(filmObject.containsKey("film_budget"));
            Object filmBudget = filmObject.get("film_budget");
            validateNumber(filmBudget);
        });

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

        assertTrue(foundKnownFilm);
    }

    @Test
    @DisplayName("Map with list output")
    void test_actorFilmographyRetrievalWithStructuredObjectWithArray() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("actor")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("filmography")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().objectSchema(Map.of(
                                "name", PortSchema.builder().stringSchema().build(),
                                "date of birth", PortSchema.builder().stringSchema().build(),
                                "films", PortSchema.builder().arraySchema(
                                        PortSchema.builder().objectSchema(Map.of(
                                                "film_name", PortSchema.builder().stringSchema().build(),
                                                "film_year", PortSchema.builder().intSchema().build(),
                                                "film_budget", PortSchema.builder().floatSchema().build()
                                        )).build()
                                ).build(),
                                "parents", PortSchema.builder().arraySchema(PortSchema.builder().stringSchema().build()).build()
                        )).build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate("""
        You are a film expert. When given an actor's name, complete its profile.
        """);

        context.put("actor", "Tom Hanks");

        llmNodeInstance.process(context);

        Object response = context.get("filmography");
        assertNotNull(response);
        assertInstanceOf(Map.class, response);

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) response;

        assertEquals("Tom Hanks", profile.get("name"));
        assertTrue(profile.containsKey("date of birth"));
        validateString(profile.get("date of birth"));

        assertTrue(profile.containsKey("films"));
        Object filmsObj = profile.get("films");
        assertInstanceOf(List.class, filmsObj);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> films = (List<Map<String, Object>>) filmsObj;
        assertFalse(films.isEmpty());

        Map<String, Object> firstFilm = films.get(0);
        assertTrue(firstFilm.containsKey("film_name"));
        assertTrue(firstFilm.containsKey("film_year"));
        assertTrue(firstFilm.containsKey("film_budget"));

        for (Map<String, Object> film : films) {
            validateString(film.get("film_name"));
            validateInt(film.get("film_year"));
            assertTrue(((int) film.get("film_year")) > 0);
            validateNumber(film.get("film_budget"));

        }

        assertInstanceOf(List.class, profile.get("parents"));
        List<?> parents = (List<?>) profile.get("parents");

        assertEquals(2, parents.size());

        for (Object parent : parents) {
            validateString(parent);
        }
    }

    @Test
    @DisplayName("List of int output")
    void test_arrayOutput_returnsListOfInt() {
        metamodel.setInputPorts(List.of(
                LlmPort.builder()
                        .withKey("MIN")
                        .withRole(LlmPort.LlmPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().intSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("res")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().intSchema().build()
                        ).build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate(
                """
                You are a fortune-teller. Give me 3 numbers greater then {MIN}
                """);

        context.put("MIN", 10);

        llmNodeInstance.process(context);

        Object response = context.get("res");
        assertNotNull(response);
        assertInstanceOf(List.class, response);
        List<?> list =  (List<?>) response;

        for (var el : list) validateInt(el);
    }


    @Test
    @DisplayName("List of floats output")
    void test_arrayOutput_returnsListOfFloat() {
        metamodel.setInputPorts(List.of());

        metamodel.setOutputPorts(List.of(
                LlmPort.builder()
                        .withKey("res")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().floatSchema().build()
                        ).build())
                        .build()
        ));

        metamodel.setSystemPromptTemplate(
                """
                You are a fortune-teller. Give me 3 numbers between 0 and 1
                """);

        llmNodeInstance.process(context);

        Object response = context.get("res");
        assertNotNull(response);
        assertInstanceOf(List.class, response);
        List<?> list =  (List<?>) response;

        for (var el : list) validateFloat(el);
    }

    private void validateString(Object obj){
        assertNotNull(obj);
        assertInstanceOf(String.class, obj);
        String str = (String) obj;
        assertFalse(str.contains("\""));
        assertFalse(((String)obj).isEmpty());
    }

    private void validateFloat(Object obj) {
        assertNotNull(obj);
        assertTrue(obj instanceof Float || obj instanceof Double,
                "Object must be a Float or Double");
    }

    private void validateNumber(Object obj) {
        assertNotNull(obj);
        assertInstanceOf(Number.class, obj);
    }

    private void validateInt(Object obj) {
        assertNotNull(obj);
        assertInstanceOf(Integer.class, obj);
    }
}