package org.caselli.cognitiveworkflow.operational.AI.services;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@ActiveProfiles("test")
public class EmbeddingServiceIT {

    @Autowired private EmbeddingService embeddingService;


    @Test
    public void testGetEmbeddings() {
        String text = "Hello, world!";
        var response = embeddingService.generateEmbedding(text);
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }



}