package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentSearchService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.junit.jupiter.api.BeforeAll;
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
@ActiveProfiles("test")

class IntentDetectorServiceIT {


    @Autowired private IntentDetectorService intentDetectorService;


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
    private static IntentMetamodel bookHotelMockIntent;

    @BeforeAll()
    static void setUp() {
        mockIntentMetamodels = new ArrayList<>();
        bookFightMockIntent = new IntentMetamodel();
        bookFightMockIntent.setId("1");
        bookFightMockIntent.setName("BOOK_FLIGHT");
        bookFightMockIntent.setDescription("Book a flight to a destination");
        mockIntentMetamodels.add(bookFightMockIntent);

        bookHotelMockIntent = new IntentMetamodel();
        bookHotelMockIntent.setId("2");
        bookHotelMockIntent.setName("BOOK_HOTEL");
        bookHotelMockIntent.setDescription("Book a hotel in a destination");
        mockIntentMetamodels.add(bookHotelMockIntent);
    }


    @Test
    void shouldHandleMultipleIntentsProperly() {

        String userRequest = "I want to book a flight to Paris for tomorrow";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        IntentDetectorResult result = intentDetectorService.detect(userRequest);
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
    void shouldCreateANewIntentIfNotExists(){
        String userRequest = "I want to book an uber car ride to my home";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        IntentDetectorResult result = intentDetectorService.detect(userRequest);

        System.out.println(result);

        assertNotNull(result);
        assertTrue(result.isNew());
        assertNull(result.getIntentId());
        // The intent name should include "UBER" in it
        assertTrue(result.getIntentName().contains("UBER"));
        // The intent name should be uppercase
        assertEquals(result.getIntentName(), result.getIntentName().toUpperCase());


    }

    @Test
    void shouldReturnNullIfInputIsNonSense(){
        String userRequest = "oajadfjaoifj";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        IntentDetectorResult result = intentDetectorService.detect(userRequest);

        assertNull(result);
    }

    @Test
    void shouldReturnNullIfInputIsNotAnIntent(){
        String userRequest = "Sometimes I like to eat a book";

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(mockIntentMetamodels);

        IntentDetectorResult result = intentDetectorService.detect(userRequest);

        assertNull(result);
    }
}
