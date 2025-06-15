package org.caselli.cognitiveworkflow.operational.instances;

import org.caselli.cognitiveworkflow.knowledge.model.node.EmbeddingsNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.EmbeddingsPort;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.caselli.cognitiveworkflow.operational.LLM.factories.EmbeddingModelFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("it")
@ActiveProfiles("test")
public class EmbeddingsNodeInstanceIT {

    @Autowired private EmbeddingModelFactory llmEmbeddingsFactory;
    private EmbeddingsNodeInstance nodeInstance;
    private ExecutionContext context;
    private EmbeddingsNodeMetamodel metamodel;

    @BeforeEach
    void setUp() {
        nodeInstance = new EmbeddingsNodeInstance(llmEmbeddingsFactory);
        metamodel = new EmbeddingsNodeMetamodel();
        metamodel.setProvider("openai");
        metamodel.setModelName("text-embedding-3-small");

        nodeInstance.setMetamodel(metamodel);
        nodeInstance.setId("test-node");
        context = new ExecutionContext();
    }

    @Test
    @DisplayName("Empty input should return empty embeddings")
    void test_emptyInput_returnsEmptyEmbeddings() {
        metamodel.setInputPorts(new ArrayList<>());

        metamodel.setOutputPorts(List.of(
                EmbeddingsPort.builder()
                        .withKey("res")
                        .withRole(EmbeddingsPort.EmbeddingsPortRole.OUTPUT_VECTOR)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        nodeInstance.process(context);
        Object res = context.get("res");
        assertNotNull(res);
        assertInstanceOf(List.class, res);
        assertTrue(((List<?>) res).isEmpty());
    }

    @Test
    @DisplayName("Simple String output")
    void test_WithTextOutput_returnsString() {
        metamodel.setInputPorts(List.of(
                EmbeddingsPort.builder()
                        .withKey("text")
                        .withRole(EmbeddingsPort.EmbeddingsPortRole.INPUT_TEXT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                EmbeddingsPort.builder()
                        .withKey("res")
                        .withRole(EmbeddingsPort.EmbeddingsPortRole.OUTPUT_VECTOR)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        context.put("text", "the sky is blue");

        nodeInstance.process(context);

        var res = context.get("res");
        assertInstanceOf(List.class, res);

        @SuppressWarnings("unchecked")
        List<?> response = (List<Number>) res;

        assertNotNull(response);
        assertFalse(response.isEmpty(), "Embedding response should not be empty");

        // Check the dimensions of the embedding (For text-embedding-3-small, expect 1536 dimensions)
        assertEquals(1536, response.size(), "Embedding should have the correct number of dimensions");

        System.out.println("Embedding vector" + response);
        System.out.println("Embedding vector size: " + response.size());
    }
}