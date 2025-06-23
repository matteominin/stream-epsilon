package org.caselli.cognitiveworkflow.operational.AI.services;


import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentSearchService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest
@Tag("it")
@Tag("focus")
@ActiveProfiles("test")

class IntentDetectionServiceIT {


    @Autowired private IntentDetectionService intentDetectionService;


    @Autowired private IntentMetamodelService intentMetamodelService;

    // Mock intent metamodel service
    @TestConfiguration
    static class TestConfig {
        @Bean
        public IntentMetamodelService intentMetamodelService() {
            return mock(IntentMetamodelService.class);
        }

        @Bean
        public IntentSearchService intentSearchService() {
            return mock(IntentSearchService.class);
        }
    }

    private static List<IntentMetamodel> mockIntentMetamodels;
    private static IntentMetamodel bookFightMockIntent;
    private static IntentMetamodel mockIntent;

    @BeforeAll()
    static void setUp() {

        mockIntentMetamodels = new ArrayList<>();
        bookFightMockIntent = new IntentMetamodel();
        bookFightMockIntent.setId("1");
        bookFightMockIntent.setName("BOOK_FLIGHT");
        bookFightMockIntent.setDescription("Book a flight to a destination");
        mockIntentMetamodels.add(bookFightMockIntent);

        mockIntent = new IntentMetamodel();
        mockIntent.setId("2");
        mockIntent.setName("BOOK_HOTEL");
        mockIntent.setDescription("Book a hotel in a destination");
        mockIntentMetamodels.add(mockIntent);

    }


    @Test
    void shouldHandleMultipleIntentsProperly() {

        String userRequest = "I want to book a flight to Paris for tomorrow";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;
        
        System.out.println(result);

        assertNotNull(result);
        assertEquals(bookFightMockIntent.getId(), result.getIntentId());
        assertEquals(bookFightMockIntent.getName(), result.getIntentName());
        assertFalse(result.isNew());
        assertTrue(result.getConfidence() >= 0);

        // Check variables. At list Paris and Tomorrow should be present among the values.
        Map<String, Object> variables = result.getUserVariables();
        assertTrue(variables.values().stream().anyMatch(value -> value.toString().toLowerCase().contains("paris")));
        assertTrue(variables.values().stream().anyMatch(value -> value.toString().toLowerCase().contains("tomorrow")));
    }

    @Test
    void shouldCreateANewIntentIfNotExists_1(){
        String userRequest = "Buy me a ticket for the concert of the band The Beatles in London next week";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;
        System.out.println(result);

        assertNotNull(result);
        assertTrue(result.isNew());
        assertNull(result.getIntentId());

        assertTrue(
                result.getIntentName().contains("PURCHASE") ||
                        result.getIntentName().contains("BUY") ||
                        result.getIntentName().contains("TICKET")
        );
        // The intent name should be uppercase
        assertEquals(result.getIntentName(), result.getIntentName().toUpperCase());


    }

    @Test
    void shouldCreateANewIntentIfNotExists_2(){
        String userRequest = "I want to translate this text to spanish";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;
        System.out.println(result);

        assertNotNull(result);
        assertTrue(result.isNew());
        assertNull(result.getIntentId());
        assertTrue(result.getIntentName().contains("TRANSLATE"));
        assertEquals(result.getIntentName(), result.getIntentName().toUpperCase());
    }

    @Test
    void shouldCreateANewIntentIfNotExists_3(){
        String userRequest = "Find me the best route using dijkstra algorithm to connect the following points: A, B, C, D. Edges: (A, B, 1), (A, C, 2), (B, C, 3), (C, D, 4)";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;

        System.out.println(result);

        assertNotNull(result);
        assertTrue(result.isNew());
        assertNull(result.getIntentId());
        assertTrue(result.getIntentName().contains("FIND") ||
                result.getIntentName().contains("ROUTE") ||
                result.getIntentName().contains("DIJKSTRA")
        );
        assertEquals(result.getIntentName(), result.getIntentName().toUpperCase());
    }


    @Test
    void shouldReturnNullIfInputIsNonSense(){
        String userRequest = "oajadfjaoifj";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;

        assertNull(result);
    }

    @Test
    void shouldReturnNullIfInputIsNotAnIntent_1(){
        String userRequest = "Sometimes I like to eat a book";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;

        assertNull(result);
    }

    @Test
    void shouldReturnNullIfInputIsNotAnIntent_2(){
        String userRequest = "Cars are better than bikes";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        var result = intentDetectionService.detect(userRequest).result;

        assertNull(result);
    }


    @Test
    void shouldMapVariablesCorrectlyForAI4NE(){
        String userRequest = "I want to establish a Real-time translation connection to Marco with 4k resolution and low latency";

        mockIntent = new IntentMetamodel();
        mockIntent.setId("1");
        mockIntent.setName("ROUTE_SERVICE_REQUEST");
        mockIntent.setDescription("Route user requests across different nodes (or cloud resources) according to user ``intent'' and hardware availability,  relying on specialized AI modules to optimize networking decisions. Supporting different request types like streaming, real-time translation, IoT, etc.");
        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(List.of(mockIntent));

        var result = intentDetectionService.detect(userRequest).result;
        System.out.println(result);

        assertNotNull(result);
        assertFalse(result.isNew());
        assertEquals(mockIntent.getId(), result.getIntentId());
        assertEquals(result.getIntentName(), mockIntent.getName());

        // 3 variables at least should be present in the result
        assertTrue(result.getUserVariables().size() >= 3);

        // Check if the variables contain the expected values
        List<String> target = List.of("Marco", "4k", "low");
        assertTrue(result.getUserVariables().values().stream().anyMatch(value -> target.stream().anyMatch(t -> value.toString().toLowerCase().contains(t.toLowerCase()))));

    }
}
