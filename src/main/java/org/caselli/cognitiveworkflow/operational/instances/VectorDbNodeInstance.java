package org.caselli.cognitiveworkflow.operational.instances;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.VectorDbNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.VectorDbPort;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@Scope("prototype")
public class VectorDbNodeInstance extends ToolNodeInstance {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MongoClient mongoClient;
    private MongoTemplate mongoTemplate;

    @Override
    public VectorDbNodeMetamodel getMetamodel() {
        return (VectorDbNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof VectorDbNodeMetamodel)) throw new IllegalArgumentException("VectorDbNodeInstance requires VectorDbNodeMetamodel");
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) {
        logger.info("[Node {}]: Processing Vector Search request.", getId());

        // INIT the DB
        // TODO: Opening and Closing connections each time is very expensive
        // We could implement a Connection Manager Service for caching pooled clients
        initializeConnection();

        try {
            VectorDbNodeMetamodel metamodel = getMetamodel();
            String collectionName = metamodel.getCollectionName();
            String indexName = metamodel.getIndexName();
            String vectorField = metamodel.getVectorField();

            // Get Input Vector
            var vector = getVectorFromContext(context);
            if (vector == null || vector.isEmpty()) {
                logger.error("[Node {}]: No input vector provided for search.", getId());
                throw new IllegalArgumentException("Vector search requires an input vector");
            }

            // Get Search Params
            var searchConfig = determineSearchParameters();

            // Execute search
            var searchResults = executeVectorSearch(collectionName, indexName, vectorField, vector, searchConfig);

            // Save results to the context
            processResultsToContext(context, searchResults);

            logger.info("[Node {}]: Vector search processed successfully.", getId());
        } finally {
            closeConnection();
        }
    }

    /**
     * Initializes a MongoDB connection
     */
    private void initializeConnection() {
        try {
            VectorDbNodeMetamodel metamodel = getMetamodel();
            String connectionString = metamodel.getUri();
            String databaseName = metamodel.getDatabaseName();

            if (connectionString == null || connectionString.isEmpty()) throw new IllegalStateException("MongoDB connection string not configured for this node");
            if (databaseName == null || databaseName.isEmpty()) throw new IllegalStateException("MongoDB database name not configured for this node");


            // Create MongoDB client
            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connString).build();
            mongoClient = MongoClients.create(settings);
            SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
            mongoTemplate = new MongoTemplate(factory);

            logger.info("[Node {}]: Successfully initialized MongoDB connection to database: {}", getId(), databaseName);

        } catch (Exception e) {
            logger.error("[Node {}]: Failed to initialize MongoDB connection: {}", getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MongoDB connection", e);
        }
    }

    /**
     * Closes the MongoDB connection
     */
    private void closeConnection() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                logger.info("[Node {}]: MongoDB connection closed.", getId());
            } catch (Exception e) {
                logger.warn("[Node {}]: Error while closing MongoDB connection: {}", getId(), e.getMessage());
            } finally {
                mongoClient = null;
                mongoTemplate = null;
            }
        }
    }

    /**
     * Extracts the vector data from input ports with role INPUT_VECTOR
     * Tries to handle different types of vectors
     * @param context The execution context
     * @return List<Double> representing the vector to search with
     */
    private List<Double> getVectorFromContext(ExecutionContext context) {
        List<VectorDbPort> inputPorts = getMetamodel().getInputPorts();

        for (VectorDbPort inputPort : inputPorts) {
            if (inputPort.getRole() == VectorDbPort.VectorDbPortRole.INPUT_VECTOR) {
                Object vectorValue = context.get(inputPort.getKey());
                if (vectorValue != null) {
                    if (vectorValue instanceof List<?>) {
                        try {
                            @SuppressWarnings("unchecked")
                            List<Double> typedVector = (List<Double>) vectorValue;
                            return typedVector;
                        } catch (ClassCastException e) {
                            logger.warn("[Node {}]: Value for port '{}' is not a List<Double>.", getId(), inputPort.getKey(), e);
                        }
                    } else if (vectorValue instanceof double[] array) {
                        List<Double> list = new ArrayList<>(array.length);
                        for (double value : array) list.add(value);
                        return list;
                    } else if (vectorValue instanceof float[] array) {
                        List<Double> list = new ArrayList<>(array.length);
                        for (float value : array) list.add((double) value);
                        return list;
                    } else if (vectorValue instanceof String) {
                        try {
                            // Tries to parse the string as a list of values separated by commas
                            String[] parts = ((String) vectorValue).split(",");
                            List<Double> list = new ArrayList<>(parts.length);
                            for (String part : parts) list.add(Double.parseDouble(part.trim()));
                            return list;
                        } catch (NumberFormatException e) {
                            logger.warn("[Node {}]: Could not parse string value for port '{}' as vector of numbers.", getId(), inputPort.getKey(), e);
                        }
                    } else {
                        logger.warn("[Node {}]: Value for port '{}' is not a supported vector type: {}", getId(), inputPort.getKey(), vectorValue.getClass().getName());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get Search parameters from the metamodel
     * @return VectorSearchConfig with the appropriate parameters
     */
    private VectorDbNodeMetamodel.VectorSearchConfig determineSearchParameters() {
        VectorDbNodeMetamodel.VectorSearchConfig config = getMetamodel().getParameters();
        if (config == null) config = new VectorDbNodeMetamodel.VectorSearchConfig();

        // TODO Look for parameter overrides in the context

        return config;
    }

    /**
     * Executes the vector search
     * @param collectionName The collection to search in
     * @param indexName The vector index name
     * @param vectorField The field containing vectors
     * @param vector The query vector
     * @param searchConfig Search configuration parameters
     * @return List of documents matching the search criteria
     */
    private List<Document> executeVectorSearch(
            String collectionName,
            String indexName,
            String vectorField,
            List<Double> vector,
            VectorDbNodeMetamodel.VectorSearchConfig searchConfig) {

        try {
            List<AggregationOperation> pipeline = new ArrayList<>();

            // Vector Search stage
            Document vectorSearchStage = buildVectorSearchStage(indexName, vectorField, vector, searchConfig);
            pipeline.add(context -> vectorSearchStage);

            // If a threshold was provided
            if(searchConfig.getThreshold() != null) {
                Document metadataStage = new Document("$addFields", new Document("score", new Document("$meta", "vectorSearchScore")));
                Document matchStage = new Document("$match", new Document("score", new Document("$gte", searchConfig.getThreshold())));
                pipeline.add(context -> metadataStage);
                pipeline.add(context -> matchStage);
            }

            // Projection stage: remove in the result the embeddings field
            Document projectStage = new Document("$project", new Document(vectorField, 0));
            pipeline.add(context -> projectStage);


            // Execute the pipeline
            Aggregation aggregation = Aggregation.newAggregation(pipeline);
            AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, collectionName, Document.class);

            List<Document> resultList = results.getMappedResults();
            logger.info("[Node {}]: Vector search returned {} results", getId(), resultList.size());
            return resultList;
        } catch (Exception e) {
            logger.error("[Node {}]: Error executing vector search: {}", getId(), e.getMessage(), e);
            throw new RuntimeException("Vector search execution failed", e);
        }
    }

    /**
     * Builds the vector search stage document for the aggregation
     */
    private Document buildVectorSearchStage(
        String indexName,
        String vectorField,
        List<Double> vector,
        VectorDbNodeMetamodel.VectorSearchConfig searchConfig) {

        int limit = searchConfig.getLimit() > 0 ? searchConfig.getLimit() : 10;

        return new Document("$vectorSearch", new Document("queryVector", vector)
                .append("path", vectorField)
                .append("numCandidates", limit * 10)
                .append("limit", limit)
                .append("index", indexName));
    }

    /**
     * Processes search results and stores them in the execution context based on output port roles
     * @param context The execution context
     * @param results The search results
     */
    private void processResultsToContext(ExecutionContext context, List<Document> results) {
        List<VectorDbPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts == null || outputPorts.isEmpty()) {
            logger.info("[Node {}]: No output ports defined.", getId());
            return;
        }

        for (VectorDbPort outputPort : outputPorts) {
            Object valueToSet = null;
            VectorDbPort.VectorDbPortRole role = outputPort.getRole();
            PortSchema portSchema = outputPort.getSchema();

            if (role == null) {
                logger.warn("[Node {}]: Output port '{}' has no role defined. This port will be ignored.", getId(), outputPort.getKey());
                continue;
            }

            switch (role) {
                case RESULTS:
                    valueToSet = mapDocumentsToSchema(results, portSchema);
                    break;

                case FIRST_RESULT:
                    valueToSet = (results != null && !results.isEmpty()) ?
                            mapDocumentToSchema(results.get(0), portSchema)
                            : null;
                    break;

                default:
                    logger.warn("[Node {}]: Output port '{}' has unexpected role: {}. This port will be ignored.", getId(), outputPort.getKey(), outputPort.getRole());
                    break;
            }

            context.put(outputPort.getKey(), valueToSet);
            logger.info("[Node {}]: Set output port '{}' with value type: {}", getId(), outputPort.getKey(), valueToSet != null ? valueToSet.getClass().getSimpleName() : "null");
        }
    }


    /**
     * Helper method to map a MongoDB document to a port schema
     * @param document MongoDB Document to map
     * @param schema Target schema to map to
     * @return Object that conforms to the schema (if possible)
     */
    private Object mapDocumentToSchema(org.bson.Document document, PortSchema schema) {
        return PortSchema.mapToSchema(document, schema);
    }

    /**
     * Convenience method to map a list of documents to a port schema
     * @param documents List of MongoDB Documents to map
     * @param schema Target schema to map to
     * @return List of objects that conform to the schema
     */
    private Object mapDocumentsToSchema(List<org.bson.Document> documents, PortSchema schema) {
        if (schema.getType() != PortType.ARRAY) {
            if (schema.getType() == PortType.STRING && documents != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.writeValueAsString(documents);
                } catch (Exception e) {
                    logger.warn("[Node {}]: Failed to convert document list to JSON string: {}", getId(), e.getMessage());
                    return documents.toString();
                }
            }
            logger.warn("[Node {}]: Target schema must be of type ARRAY when mapping multiple documents", getId());
            return null;
        }

        List<Object> results = new ArrayList<>();

        if (documents != null)
            for (org.bson.Document doc : documents)
                results.add(PortSchema.mapToSchema(doc, schema.getItems()));

        return results;
    }
}