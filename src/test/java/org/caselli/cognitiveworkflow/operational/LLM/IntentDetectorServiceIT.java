package org.caselli.cognitiveworkflow.operational.LLM;

import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentSearchService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")

class IntentDetectorServiceIT {


    @Autowired private IntentDetectorService intentDetectorService;


    @Autowired private IntentMetamodelService intentMetamodelService;


    @Test
    void shouldHandleMultipleIntentsProperly() {

        String userRequest = "I want to book a flight to Paris for tomorrow";


        // MOCK INTENTS RAG RESULTS
        List<IntentMetamodel> existingIntent = new ArrayList<IntentMetamodel>();
        IntentMetamodel bookFightIntent = new IntentMetamodel();
        bookFightIntent.setId("1");
        bookFightIntent.setDescription("Book a flight to a destination");
        bookFightIntent.setName("BOOK_FLIGHT");
        existingIntent.add(bookFightIntent);

        IntentMetamodel bookHotelIntent = new IntentMetamodel();
        bookHotelIntent.setId("2");
        bookHotelIntent.setDescription("Book a hotel in a destination");
        bookHotelIntent.setName("BOOK_HOTEL");
        existingIntent.add(bookHotelIntent);

        when(intentMetamodelService.findMostSimilarIntent(userRequest)).thenReturn(existingIntent);

        IntentDetectorResult result = intentDetectorService.detect(userRequest);

        assertEquals(bookHotelIntent.getId(), result.getIntentId());
        assertEquals(bookHotelIntent.getName(), result.getIntentName());




    }
}
