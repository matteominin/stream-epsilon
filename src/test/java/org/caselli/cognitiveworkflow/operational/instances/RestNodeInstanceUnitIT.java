package org.caselli.cognitiveworkflow.operational.instances;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortSchema;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.springframework.test.context.ActiveProfiles;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Tag("it")
@ActiveProfiles("test")
class RestNodeInstanceIT {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance().build();

    private RestNodeInstance restNodeInstance;
    private ExecutionContext context;
    private RestNodeMetamodel metamodel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restNodeInstance = new RestNodeInstance();
        context = new ExecutionContext();
        metamodel = new RestNodeMetamodel();
        restNodeInstance.setMetamodel(metamodel);
        restNodeInstance.setId("testNode");
    }

    @Test

    @DisplayName("Should handle basic GET request with no inputs")
    void shouldHandleBasicGetRequest() {
        String testPath = "/api/resource";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"status\":\"success\"}")));


        RestPort outputPort = new RestPort();
        outputPort.setKey("outputBody");
        outputPort.setRole(RestPort.RestPortRole.RES_FULL_BODY);
        outputPort.setSchema(PortSchema.builder().stringSchema().build());

        RestPort statusOutputPort = new RestPort();
        statusOutputPort.setKey("outputStatus");
        statusOutputPort.setRole(RestPort.RestPortRole.RES_STATUS);
        RestPort headersOutputPort = new RestPort();
        headersOutputPort.setKey("outputHeaders");
        headersOutputPort.setRole(RestPort.RestPortRole.RES_HEADERS);
        metamodel.setOutputPorts(Arrays.asList(outputPort, statusOutputPort, headersOutputPort));

        restNodeInstance.process(context);

        wireMockServer.verify(getRequestedFor(urlEqualTo(testPath)));

        assertEquals("{\"status\":\"success\"}", context.get("outputBody"));
        assertEquals(200, context.get("outputStatus"));

        @SuppressWarnings("unchecked")
        Map<String, String> receivedHeaders = (Map<String, String>) context.get("outputHeaders");
        assertNotNull(receivedHeaders);
        assertEquals(MediaType.APPLICATION_JSON_VALUE, receivedHeaders.get("Content-Type"));
    }

    @Test
    @DisplayName("Should handle GET request with query parameters")
    void shouldHandleGetRequestWithQueryParams() {
        String testPath = "/api/search";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        RestPort queryParamPort1 = new RestPort();
        queryParamPort1.setKey("query");
        queryParamPort1.setRole(RestPort.RestPortRole.REQ_QUERY_PARAMETER);
        RestPort queryParamPort2 = new RestPort();
        queryParamPort2.setKey("page");
        queryParamPort2.setRole(RestPort.RestPortRole.REQ_QUERY_PARAMETER);
        metamodel.setInputPorts(Arrays.asList(queryParamPort1, queryParamPort2));

        context.put("query", "test search");
        context.put("page", 1);

        wireMockServer.stubFor(get(urlEqualTo(testPath + "?query=test+search&page=1")).willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        restNodeInstance.process(context);

        wireMockServer.verify(getRequestedFor(urlEqualTo(testPath + "?query=test+search&page=1")));
    }

    @Test
    @DisplayName("Should handle GET request with path variables")
    void shouldHandleGetRequestWithPathVariables() {
        String baseUri = "/api/users/{userId}/posts/{postId}";
        metamodel.setUri(wireMockServer.baseUrl() + baseUri);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        RestPort pathVarPort1 = new RestPort();
        pathVarPort1.setKey("userId");
        pathVarPort1.setRole(RestPort.RestPortRole.REQ_PATH_VARIABLE);
        RestPort pathVarPort2 = new RestPort();
        pathVarPort2.setKey("postId");
        pathVarPort2.setRole(RestPort.RestPortRole.REQ_PATH_VARIABLE);
        metamodel.setInputPorts(Arrays.asList(pathVarPort1, pathVarPort2));

        context.put("userId", "123");
        context.put("postId", "abc");

        String expectedPath = "/api/users/123/posts/abc";
        wireMockServer.stubFor(get(urlEqualTo(expectedPath)).willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        restNodeInstance.process(context);

        wireMockServer.verify(getRequestedFor(urlEqualTo(expectedPath)));
    }

    @Test
    @DisplayName("Should handle POST request with REQ_BODY and REQ_BODY_FIELD")
    void shouldHandlePostRequestWithBody() throws Exception {
        String testPath = "/api/create";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.POST);

        RestPort reqBodyPort = new RestPort();
        reqBodyPort.setKey("baseData");
        reqBodyPort.setRole(RestPort.RestPortRole.REQ_BODY);

        RestPort reqBodyFieldPort1 = new RestPort();
        reqBodyFieldPort1.setKey("name");
        reqBodyFieldPort1.setRole(RestPort.RestPortRole.REQ_BODY_FIELD);

        RestPort reqBodyFieldPort2 = new RestPort();
        reqBodyFieldPort2.setKey("age");
        reqBodyFieldPort2.setRole(RestPort.RestPortRole.REQ_BODY_FIELD);

        metamodel.setInputPorts(Arrays.asList(reqBodyPort, reqBodyFieldPort1, reqBodyFieldPort2));

        Map<String, Object> baseData = new HashMap<>();
        baseData.put("id", 1);
        baseData.put("status", "active");

        context.put("baseData", baseData);
        context.put("name", "John Doe");
        context.put("age", 30);

        Map<String, Object> expectedBody = new HashMap<>();
        expectedBody.put("id", 1);
        expectedBody.put("status", "active");
        expectedBody.put("name", "John Doe");
        expectedBody.put("age", 30);
        String expectedJsonBody = objectMapper.writeValueAsString(expectedBody);

        wireMockServer.stubFor(post(urlEqualTo(testPath))
                .withRequestBody(equalToJson(expectedJsonBody, true, false)) // true for ignoreArrayOrder, false for ignoreExtraElements
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"message\":\"created\"}")));

        restNodeInstance.process(context);


        wireMockServer.verify(postRequestedFor(urlEqualTo(testPath)).withRequestBody(equalToJson(expectedJsonBody, true, false)));
    }

    @Test
    @DisplayName("Should handle request headers from default, REQ_HEADER, and REQ_HEADER_FIELD")
    void shouldHandleRequestHeaders() {
        String testPath = "/api/secured";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        // Default headers from metamodel
        Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put("Authorization", "Bearer default_token");
        defaultHeaders.put("Accept", "application/json");
        metamodel.setHeaders(defaultHeaders);

        // REQ_HEADER port
        RestPort reqHeaderPort = new RestPort();
        reqHeaderPort.setKey("dynamicHeaders");
        reqHeaderPort.setRole(RestPort.RestPortRole.REQ_HEADER);

        // REQ_HEADER_FIELD port
        RestPort reqHeaderFieldPort = new RestPort();
        reqHeaderFieldPort.setKey("X-Custom-Field");
        reqHeaderFieldPort.setRole(RestPort.RestPortRole.REQ_HEADER_FIELD);

        RestPort overridingAuthHeaderField = new RestPort();
        overridingAuthHeaderField.setKey("Authorization");
        overridingAuthHeaderField.setRole(RestPort.RestPortRole.REQ_HEADER_FIELD);

        metamodel.setInputPorts(Arrays.asList(reqHeaderPort, reqHeaderFieldPort, overridingAuthHeaderField));

        Map<String, String> dynamicHeaders = new HashMap<>();
        dynamicHeaders.put("Content-Type", "application/json");
        dynamicHeaders.put("Accept", "text/plain");
        context.put("dynamicHeaders", dynamicHeaders);
        context.put("X-Custom-Field", "my_custom_value");
        context.put("Authorization", "Bearer new_token");

        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        restNodeInstance.process(context);

        wireMockServer.verify(getRequestedFor(urlEqualTo(testPath))
                .withHeader("Authorization", equalTo("Bearer new_token"))
                .withHeader("Accept", equalTo("text/plain"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-Custom-Field", equalTo("my_custom_value")));
    }

    @Test
    @DisplayName("Should handle response status, full body, and body fields")
    void shouldHandleResponseOutputs() {
        String testPath = "/api/data";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        // Output ports
        RestPort fullBodyPort = new RestPort();
        fullBodyPort.setKey("responseFullBody");
        fullBodyPort.setRole(RestPort.RestPortRole.RES_FULL_BODY);
        fullBodyPort.setSchema(PortSchema.builder().stringSchema().build());

        RestPort statusPort = new RestPort();
        statusPort.setKey("responseStatus");
        statusPort.setRole(RestPort.RestPortRole.RES_STATUS);
        fullBodyPort.setSchema(PortSchema.builder().stringSchema().build());

        RestPort headersPort = new RestPort();
        headersPort.setKey("responseHeaders");
        headersPort.setRole(RestPort.RestPortRole.RES_HEADERS);
        fullBodyPort.setSchema(PortSchema.builder().stringSchema().build());

        RestPort nameFieldPort = new RestPort();
        nameFieldPort.setKey("name");
        nameFieldPort.setRole(RestPort.RestPortRole.RES_BODY_FIELD);
        fullBodyPort.setSchema(PortSchema.builder().stringSchema().build());

        RestPort cityFieldPort = new RestPort();
        cityFieldPort.setKey("city");
        cityFieldPort.setRole(RestPort.RestPortRole.RES_BODY_FIELD);
        fullBodyPort.setSchema(PortSchema.builder().stringSchema().build());

        metamodel.setOutputPorts(Arrays.asList(fullBodyPort, statusPort, headersPort, nameFieldPort, cityFieldPort));

        String mockResponseBody = "{\"id\":123,\"name\":\"Alice\",\"city\":\"New York\",\"age\":25}";
        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("custom-header", "custom-value")
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(mockResponseBody)));

        restNodeInstance.process(context);

        wireMockServer.verify(getRequestedFor(urlEqualTo(testPath)));

        // Verify outputs in context
        assertEquals(mockResponseBody, context.get("responseFullBody"));
        assertEquals(HttpStatus.OK.value(), context.get("responseStatus"));

        @SuppressWarnings("unchecked")
        Map<String, String> actualHeaders = (Map<String, String>) context.get("responseHeaders");
        assertNotNull(actualHeaders);

        assertEquals("custom-value", actualHeaders.get("custom-header"));
        assertEquals(MediaType.APPLICATION_JSON_VALUE, actualHeaders.get("Content-Type"));

        assertEquals("Alice", context.get("name"));
        assertEquals("New York", context.get("city"));
        assertNull(context.get("age")); // Not defined as an output port, so should not be in context
    }

    @Test
    @DisplayName("Should handle empty/null response body for RES_BODY_FIELD correctly")
    void shouldHandleEmptyResponseBodyForBodyField() {
        String testPath = "/api/empty";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        RestPort nameFieldPort = new RestPort();
        nameFieldPort.setKey("name");
        nameFieldPort.setRole(RestPort.RestPortRole.RES_BODY_FIELD);
        metamodel.setOutputPorts(Collections.singletonList(nameFieldPort));

        // Stub with empty string body
        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("")));

        restNodeInstance.process(context);
        assertNull(context.get("name"));

        // Stub with null body (simulating no body in response)
        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody((String) null))); // WireMock returns null body as empty string usually

        context = new ExecutionContext();
        restNodeInstance.process(context);
        assertNull(context.get("name"));
    }

    @Test
    @DisplayName("Should log warning and not parse body if RES_BODY_FIELD exists but body is malformed JSON")
    void shouldHandleMalformedJsonBody() {
        String testPath = "/api/malformed";
        metamodel.setUri(wireMockServer.baseUrl() + testPath);
        metamodel.setInvocationMethod(RestNodeMetamodel.InvocationMethod.GET);

        RestPort nameFieldPort = new RestPort();
        nameFieldPort.setKey("name");
        nameFieldPort.setRole(RestPort.RestPortRole.RES_BODY_FIELD);
        metamodel.setOutputPorts(Collections.singletonList(nameFieldPort));

        String malformedJson = "{ \"name\": \"Test\", \"age\": 30, "; // Incomplete JSON
        wireMockServer.stubFor(get(urlEqualTo(testPath))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(malformedJson)));

        restNodeInstance.process(context);
        assertNull(context.get("name"));
    }
}