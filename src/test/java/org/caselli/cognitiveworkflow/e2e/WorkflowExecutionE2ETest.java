package org.caselli.cognitiveworkflow.e2e;

import org.apache.coyote.BadRequestException;
import org.caselli.cognitiveworkflow.knowledge.MOP.IntentMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.NodeMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.MOP.WorkflowMetamodelService;
import org.caselli.cognitiveworkflow.knowledge.model.intent.IntentMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.*;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.*;
import org.caselli.cognitiveworkflow.knowledge.model.shared.Version;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowEdge;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.workflow.WorkflowNode;
import org.caselli.cognitiveworkflow.operational.LLM.services.PortAdapterService;
import org.caselli.cognitiveworkflow.operational.execution.WorkflowOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * End-to-end test class for verifying the execution of cognitive workflows,
 * specifically focusing on a Demo Movie RAG (Retrieval Augmented Generation) workflow.
 * <ul>
 * <li>{@link #testRAGWorkflowExecutionWithAllExplicitBindings()}: This test
 * verifies the workflow execution when all necessary port bindings between
 * nodes in the workflow are explicitly defined. It ensures that the workflow
 * orchestrator correctly processes the input and produces the expected output
 * without needing any dynamic port adaptation.</li>
 * <li>{@link #testRAGWorkflowExecutionWithoutExplicitBindings()}: This test
 * focuses on scenarios where port bindings between nodes are not explicitly
 * defined. It demonstrates and verifies the functionality of the
 * {@link PortAdapterService}, which automatically determines and applies
 * the correct port mappings during workflow execution. It also asserts
 * that these dynamically determined bindings are persisted in the workflow
 * metamodel for future executions.</li>
 * </ul>
 */
public class WorkflowExecutionE2ETest extends BaseE2ETest {

    @Value("${mongodb-vector-search-demo.uri}")
    private String mongodbDemoURI;

    @Autowired
    WorkflowOrchestrator workflowOrchestrator;

    @MockitoSpyBean // To Mock Vector Search
    IntentMetamodelService intentMetamodelService;

    @Autowired
    NodeMetamodelService nodeMetamodelService;

    @Autowired
    WorkflowMetamodelService workflowMetamodelService;

    @MockitoSpyBean // Spy on the adaptor service to verify its calls
    PortAdapterService workflowPortAdaptorService;

    private IntentMetamodel createIntent() {
        IntentMetamodel metamodel = new IntentMetamodel();
        metamodel.setName("FIND_MOVIE");
        metamodel.setDescription("Find a movie by the user's description");

        // Mock the intent vector search as the index creation is not real time and it is slow!
        doReturn(List.of(metamodel)).when(intentMetamodelService).findMostSimilarIntent(anyString());

        return metamodel;
    }

    private LlmNodeMetamodel createLlmNode() {
        LlmNodeMetamodel metamodel = new LlmNodeMetamodel();
        metamodel.setName("LLM Movies Matcher");
        metamodel.setDescription("Matches movies to input request");
        metamodel.setEnabled(true);
        metamodel.setAuthor("System");

        metamodel.setInputPorts(List.of(
                LlmPort.builder().withKey("movies")
                        .withRole(LlmPort.LlmPortRole.SYSTEM_PROMPT_VARIABLE)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().objectSchema(Map.of(
                                        "plot", PortSchema.builder().stringSchema().build(),
                                        "title", PortSchema.builder().stringSchema().build()
                                )).build()
                        ).withRequired(true).build())
                        .build(),

                LlmPort.builder().withKey("user_input")
                        .withRole(LlmPort.LlmPortRole.USER_PROMPT)
                        .withSchema(PortSchema.builder().stringSchema().build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                LlmPort.builder().withKey("res")
                        .withRole(LlmPort.LlmPortRole.RESPONSE)
                        .withSchema(
                                PortSchema.builder().objectSchema(Map.of(
                                        "plot", PortSchema.builder().stringSchema().build(),
                                        "title", PortSchema.builder().stringSchema().build()
                                )).withRequired(true).build()
                        ).build()
        ));

        metamodel.setProvider("openai");
        metamodel.setModelName("gpt-4o");
        metamodel.setSystemPromptTemplate(
                """
                        You are a movie matching assistant. Your task is to find which movie from the provided list best matches the user's description.

                        You will receive a list of available movies. This list contains objects, each with a 'plot' (string) and a 'title' (string).

                        The user will provide a natural language description of a movie.

                        Compare the user's description to the 'plot' of each movie in the movies in the list. Identify the movie whose 'plot' is the best match for the user's description. This is the list of movies: {movies}"""
        );

        Version version = new Version();
        version.setPatch(0);
        version.setMinor(0);
        version.setMajor(0);
        metamodel.setVersion(version);

        var options = new LlmNodeMetamodel.LlmModelOptions();
        options.setTemperature(0.4);
        options.setMaxTokens(500);
        metamodel.setParameters(options);

        return metamodel;
    }

    private VectorDbNodeMetamodel createVectorDbNode() {
        VectorDbNodeMetamodel metamodel = new VectorDbNodeMetamodel();
        metamodel.setName("Vector DB - Movies DB");
        metamodel.setDescription("DB of movies, plots, ratings");
        metamodel.setEnabled(true);
        metamodel.setAuthor("System");

        metamodel.setInputPorts(List.of(
                VectorDbPort.builder().withKey("vector")
                        .withRole(VectorDbPort.VectorDbPortRole.INPUT_VECTOR)
                        .withSchema(PortSchema.builder().arraySchema(PortSchema.builder().floatSchema().build()).withRequired(true).build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                VectorDbPort.builder().withKey("results")
                        .withRole(VectorDbPort.VectorDbPortRole.RESULTS)
                        .withSchema(PortSchema.builder().arraySchema(
                                PortSchema.builder().objectSchema(Map.of(
                                        "plot", PortSchema.builder().stringSchema().build(),
                                        "title", PortSchema.builder().stringSchema().build()
                                )).build()
                        ).build())
                        .build()
        ));

        metamodel.setUri(mongodbDemoURI);
        metamodel.setVectorField("plot_embedding");
        metamodel.setCollectionName("embedded_movies");
        metamodel.setDatabaseName("sample_mflix");
        metamodel.setIndexName("embedded_movies_vector_index");

        Version version = new Version();
        version.setPatch(0);
        version.setMinor(0);
        version.setMajor(0);
        metamodel.setVersion(version);

        return metamodel;
    }

    private EmbeddingsNodeMetamodel createEmbeddingsDbNode() {
        var metamodel = new EmbeddingsNodeMetamodel();
        metamodel.setName("Input Embeddings");
        metamodel.setDescription("Create the embeddings of the input");
        metamodel.setEnabled(true);
        metamodel.setAuthor("System");

        metamodel.setInputPorts(List.of(
                EmbeddingsPort.builder().withKey("input")
                        .withRole(EmbeddingsPort.EmbeddingsPortRole.INPUT_TEXT)
                        .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                        .build()
        ));

        metamodel.setOutputPorts(List.of(
                EmbeddingsPort.builder().withKey("output")
                        .withRole(EmbeddingsPort.EmbeddingsPortRole.OUTPUT_VECTOR)
                        .withSchema(PortSchema.builder().arraySchema(PortSchema.builder().floatSchema().build()).withRequired(true).build())
                        .build()
        ));

        metamodel.setModelName("text-embedding-ada-002");
        metamodel.setProvider("openai");

        Version version = new Version();
        version.setPatch(0);
        version.setMinor(0);
        version.setMajor(0);
        metamodel.setVersion(version);

        return metamodel;
    }

    private GatewayNodeMetamodel createGatewayNode() {
        GatewayNodeMetamodel metamodel = new GatewayNodeMetamodel();
        metamodel.setName("Starting Gateway");
        metamodel.setDescription("Forward user input");
        metamodel.setEnabled(true);
        metamodel.setAuthor("System");

        metamodel.setInputPorts(List.of(
                StandardPort.builder().withKey("input")
                        .withSchema(PortSchema.builder().stringSchema().withRequired(true).build())
                        .build()
        ));

        Version version = new Version();
        version.setPatch(0);
        version.setMinor(0);
        version.setMajor(0);
        metamodel.setVersion(version);

        return metamodel;
    }

    private WorkflowMetamodel createWorkflowMetamodel(
            IntentMetamodel intent,
            NodeMetamodel nodeMetamodel1,
            NodeMetamodel nodeMetamodel2,
            NodeMetamodel nodeMetamodel3,
            NodeMetamodel nodeMetamodel4
    ) {
        var metamodel = new WorkflowMetamodel();
        metamodel.setName("Movies RAG");
        metamodel.setDescription("Gives the title of movies by natural language description");
        metamodel.setEnabled(true);

        Version version = new Version();
        version.setPatch(0);
        version.setMinor(0);
        version.setMajor(0);
        metamodel.setVersion(version);

        WorkflowNode node1 = new WorkflowNode();
        node1.setNodeMetamodelId(nodeMetamodel1.getId());
        node1.setId("1");

        WorkflowNode node2 = new WorkflowNode();
        node2.setNodeMetamodelId(nodeMetamodel2.getId());
        node2.setId("2");

        WorkflowNode node3 = new WorkflowNode();
        node3.setNodeMetamodelId(nodeMetamodel3.getId());
        node3.setId("3");

        WorkflowNode node4 = new WorkflowNode();
        node4.setNodeMetamodelId(nodeMetamodel4.getId());
        node4.setId("4");

        metamodel.setNodes(List.of(node1, node2, node3, node4));

        WorkflowEdge edge12 = new WorkflowEdge();
        edge12.setSourceNodeId("1");
        edge12.setTargetNodeId("2");

        WorkflowEdge edge14 = new WorkflowEdge();
        edge14.setSourceNodeId("1");
        edge14.setTargetNodeId("4");
        edge14.setBindings(Map.of("input", "user_input"));

        WorkflowEdge edge23 = new WorkflowEdge();
        edge23.setSourceNodeId("2");
        edge23.setTargetNodeId("3");
        edge23.setBindings(Map.of("output", "vector"));

        WorkflowEdge edge34 = new WorkflowEdge();
        edge34.setSourceNodeId("3");
        edge34.setTargetNodeId("4");
        edge34.setBindings(Map.of("results", "movies"));

        metamodel.setEdges(List.of(edge12, edge14, edge23, edge34));

        var handledIntent = new WorkflowMetamodel.WorkflowIntentCapability();
        handledIntent.setIntentId(intent.getId());
        metamodel.setHandledIntents(List.of(handledIntent));

        return metamodel;
    }

    @Test
    @DisplayName("Test the movie RAG workflow execution without port adaptation")
    void testRAGWorkflowExecutionWithAllExplicitBindings() throws BadRequestException {
        var intent = intentMetamodelService.create(createIntent());

        var gatewayNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createGatewayNode());
        var embeddingsNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createEmbeddingsDbNode());
        var dbNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createVectorDbNode());
        var llmNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createLlmNode());

        workflowMetamodelService.createWorkflow(createWorkflowMetamodel(intent, gatewayNodeMetamodel, embeddingsNodeMetamodel, dbNodeMetamodel, llmNodeMetamodel));

        // Execute
        String request = "What is the title of the famous movie about an aristocrat?";
        var result = workflowOrchestrator.orchestrateWorkflow(request);
        var output = result.getOutput();

        System.out.println("Output: " + output);

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> resMap = (Map<String, Object>) output.get("res");
        assertTrue(resMap.containsKey("title"));
        assertTrue(resMap.containsKey("plot"));
    }


    @Test
    @DisplayName("Test the movie RAG workflow execution with port adaptation")
    void testRAGWorkflowExecutionWithoutExplicitBindings() throws BadRequestException {
        var intent = intentMetamodelService.create(createIntent());

        var gatewayNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createGatewayNode());
        var embeddingsNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createEmbeddingsDbNode());
        var dbNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createVectorDbNode());
        var llmNodeMetamodel = nodeMetamodelService.createNodeMetamodel(createLlmNode());

        var workflowMetamodel = createWorkflowMetamodel(intent, gatewayNodeMetamodel, embeddingsNodeMetamodel, dbNodeMetamodel, llmNodeMetamodel);

        // Remove all the bindings from the workflow's edges
        for (var edge : workflowMetamodel.getEdges()) edge.setBindings(null);

        // Save the workflow
        workflowMetamodelService.createWorkflow(workflowMetamodel);

        // Execute
        String request = "What is the title of the famous movie about an aristocrat?";
        var res = workflowOrchestrator.orchestrateWorkflow(request);
        var output = res.getOutput();

        System.out.println("output: " + output);

        assertThat(output).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> resMap = (Map<String, Object>) output.get("res");
        assertTrue(resMap.containsKey("title"));        assertTrue(resMap.containsKey("title"));
        assertTrue(resMap.containsKey("plot"));

        // Test that the workflow port adaptor service was called
        // The service should be called 2 times as there are two missing explicit bindings
        // between node 2 (embedding) -> 3 (db) and 3 (db) -> 4 (llm)
        verify(workflowPortAdaptorService, times(2)).adaptPorts(any(),any());

        // Test that now the workflow bindings have been saved in the model
        Optional<WorkflowMetamodel> updatedWorkflowOptional = workflowMetamodelService.getWorkflowById(workflowMetamodel.getId());
        assertThat(updatedWorkflowOptional).isPresent();
        WorkflowMetamodel updatedSavedWorkflow = updatedWorkflowOptional.get();

        assertTrue(updatedSavedWorkflow.getEdges().stream()
                .filter(e -> e.getSourceNodeId().equals("2") && e.getTargetNodeId().equals("3"))
                .findFirst().orElseThrow().getBindings()
                .containsKey("output"));

        assertTrue(updatedSavedWorkflow.getEdges().stream()
                .filter(e -> e.getSourceNodeId().equals("3") && e.getTargetNodeId().equals("4"))
                .findFirst().orElseThrow().getBindings()
                .containsKey("results"));

    }
}