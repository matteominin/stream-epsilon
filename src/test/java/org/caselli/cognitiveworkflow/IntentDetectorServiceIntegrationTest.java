package org.caselli.cognitiveworkflow;

import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Load the full Spring Boot context
@SpringBootTest
// Provide real or test configuration for your LLM.
// Use environment variables or a test-specific properties file for keys!
@TestPropertySource(properties = {
        "intent-detector.llm.provider=YOUR_LLM_PROVIDER_ID", // e.g., openai, azure-openai, google-gemini
        "intent-detector.llm.api-key=${YOUR_LLM_API_KEY_ENV_VAR}", // e.g., OPENAI_API_KEY, AZURE_OPENAI_KEY, GEMINI_API_KEY
        "intent-detector.llm.model=YOUR_LLM_MODEL_NAME", // e.g., gpt-4o, gpt-3.5-turbo, gemini-pro, etc.
        "intent-detector.llm.temperature=0.1" // Use a low temperature for more deterministic results in tests
        // Add other provider-specific properties as needed (base-url, deployment-name, etc.)
})
class IntentDetectorServiceIntegrationTest {

    @Autowired
    private IntentDetectorService intentDetectorService;

    // Mock the IntentMetamodelService, as we don't want to rely on a real DB in this test
    // We want to test the LLM interaction given a predefined set of "available intents"
    @MockBean
    private IntentMetamodelService intentMetamodelService;

    // Helper method to create a mock IntentMetamodel
    private IntentMetamodel createMockIntent(String id, String name, String description) {
        IntentMetamodel intent = new IntentMetamodel(); // Use real object if it's a simple data holder, or mock
        // If IntentMetamodel is a simple record or class with public getters/setters, you can create real instances
        // If it's a JPA entity or complex object, mocking might still be easier.
        // Let's assume it's a simple data class for integration tests.
        intent.setId(id);
        intent.setName(name);
        intent.setDescription(description);
        return intent;
    }


    @Test
    void detect_withRealLlm_existingIntentMatch() {
        // Arrange
        String userInput = "I need to check my balance";
        String existingIntentId = UUID.randomUUID().toString(); // Use a real UUID or consistent test ID
        String existingIntentName = "CHECK_BALANCE";
        IntentMetamodel mockIntent = createMockIntent(existingIntentId, existingIntentName, "Allows checking account balance.");
        List<IntentMetamodel> topIntents = Arrays.asList(
                mockIntent,
                createMockIntent(UUID.randomUUID().toString(), "TRANSFER_FUNDS", "Transfers money."),
                createMockIntent(UUID.randomUUID().toString(), "PAY_BILL", "Pays a bill.")
        );

        // Configure the mock IntentMetamodelService to return our predefined intents
        when(intentMetamodelService.findMostSimilarIntent(userInput)).thenReturn(topIntents);

        // Act
        IntentDetectorResult result = intentDetectorService.detect(userInput);

        // Assert
        assertNotNull(result, "LLM should return a result for a clear intent");
        // Check that the LLM identified the correct intent (within reason, depends on prompt effectiveness)
        assertEquals(existingIntentName, result.getIntentName(), "LLM should identify the correct intent name");
        // Check if the service correctly determined it's not a new intent
        assertFalse(result.isNew(), "Service should identify this as an existing intent");
        // Verify the service linked it to the correct ID from the mocked list
        assertEquals(existingIntentId, result.getIntentId(), "Service should link the LLM's response to the correct Intent ID from the list");
        assertTrue(result.getConfidence() > 0.5, "Confidence should be reasonably high for a clear match");
        // Add assertions for expected variables if applicable
        // assertTrue(result.getUserVariables().isEmpty(), "Expected no variables for this intent");
    }

    @Test
    void detect_withRealLlm_newIntentProposed() {
        // Arrange
        String userInput = "Tell me a joke about cats";
        // Mock intent service to return unrelated intents, indicating no strong match
        List<IntentMetamodel> topIntents = Arrays.asList(
                createMockIntent(UUID.randomUUID().toString(), "PLACE_ORDER", "..."),
                createMockIntent(UUID.randomUUID().toString(), "GET_STATUS", "...")
        );
        when(intentMetamodelService.findMostSimilarIntent(userInput)).thenReturn(topIntents);

        // Act
        IntentDetectorResult result = intentDetectorService.detect(userInput);

        // Assert
        assertNotNull(result, "LLM should return a result even for a new intent");
        assertNull(result.getIntentId(), "Should not have an ID for a new intent proposed by LLM");
        assertTrue(result.isNew(), "Service should identify this as a new intent");
        assertTrue(result.getConfidence() > 0.4, "Confidence should be reasonable for a clear but new intent");
        // You might assert that the intentName is NOT one of the existing ones
        List<String> existingNames = Arrays.asList("PLACE_ORDER", "GET_STATUS");
        assertFalse(existingNames.contains(result.getIntentName()), "LLM should propose a new intent name");
        // Check for expected variables if the new intent type implies them (less common for new intents)
    }

    @Test
    void detect_withRealLlm_nonsensicalInput() {
        // Arrange
        String userInput = "asdfghjklmnopqrstuvwxyz"; // Truly nonsensical
        List<IntentMetamodel> topIntents = Collections.emptyList(); // Or some standard list
        when(intentMetamodelService.findMostSimilarIntent(userInput)).thenReturn(topIntents);


        // Act
        IntentDetectorResult result = intentDetectorService.detect(userInput);

        // Assert
        // Based on your prompt instruction "only then return `null`", we expect null.
        assertNull(result, "LLM should return null for truly nonsensical input as per prompt");

        // Note: LLMs can sometimes struggle with this and might return a low-confidence result
        // mapping to a default intent or proposing a weird new one.
        // If you find the LLM doesn't reliably return null, you might need to adjust the prompt
        // or add a post-processing step in your service to handle very low confidence scores (<0.1)
        // as "no clear intent".
    }

    @Test
    void detect_withRealLlm_extractVariables() {
        // Arrange
        String userInput = "I want to set my alarm for 7:30 AM tomorrow";
        String existingIntentId = UUID.randomUUID().toString();
        String existingIntentName = "SET_ALARM";
        IntentMetamodel mockIntent = createMockIntent(existingIntentId, existingIntentName, "Sets a user alarm.");
        List<IntentMetamodel> topIntents = Collections.singletonList(mockIntent);
        when(intentMetamodelService.findMostSimilarIntent(userInput)).thenReturn(topIntents);

        // Act
        IntentDetectorResult result = intentDetectorService.detect(userInput);

        // Assert
        assertNotNull(result);
        assertEquals(existingIntentName, result.getIntentName());
        assertEquals(existingIntentId, result.getIntentId());
        assertFalse(result.isNew());
        assertTrue(result.getConfidence() > 0.7); // Expect high confidence

        // Check for extracted variables based on your prompt and LLM's capability
        assertNotNull(result.getUserVariables());
        // LLMs might extract variables differently. Be flexible in your assertions.
        // Example assertions assuming the LLM follows the UPPERCASE_WITH_UNDERSCORES rule:
        assertTrue(result.getUserVariables().containsKey("TIME"));
        // assertTrue(result.getUserVariables().get("TIME").toString().contains("7:30")); // Be flexible with format
        assertTrue(result.getUserVariables().containsKey("DAY"));
        // assertEquals("tomorrow", result.getUserVariables().get("DAY").toString().toLowerCase());

        // If your commented-out StringUtils formatting were active, you'd check the formatted keys here.
    }


    // Add more integration tests for various edge cases:
    // - Ambiguous input (e.g., "book something")
    // - Input that overlaps slightly with multiple intents
    // - Input with misspellings or grammatical errors
    // - Very short inputs
    // - Inputs in different languages (if LLM supports it and your service should handle it)

}