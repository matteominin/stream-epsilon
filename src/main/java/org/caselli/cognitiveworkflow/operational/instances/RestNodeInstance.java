package org.caselli.cognitiveworkflow.operational.instances;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.PortType;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Map.Entry;

@Setter
@Getter
@Component
@Scope("prototype")
public class RestNodeInstance extends ToolNodeInstance {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate restTemplate;

    public RestNodeInstance() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public RestNodeMetamodel getMetamodel() {
        return (RestNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof RestNodeMetamodel)) {
            throw new IllegalArgumentException("RestNodeInstance requires RestNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) {

        logger.info("[Node {}]: Processing REST request.", getId());

        RestNodeMetamodel metamodel = getMetamodel();
        String serviceUri = metamodel.getUri();
        RestNodeMetamodel.InvocationMethod invocationMethod = metamodel.getInvocationMethod();

        // PATH VARIABLES (e.g., /resource/{id})
        Map<String, String> pathVariables = getPathVariables(context);
        if (pathVariables != null && !pathVariables.isEmpty()) {
            for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = entry.getValue();
                serviceUri = serviceUri.replace(placeholder, value);
            }
        }

        // QUERY PARAMETERS (e.g., ?param1=value1&param2=value2)
        Map<String, String> queryParams = getQueryParams(context);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(serviceUri);
        if (queryParams != null && !queryParams.isEmpty()) queryParams.forEach(uriBuilder::queryParam);
        URI finalUri = uriBuilder.build().toUri();

        // Headers
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, String> headers = getHeader(context);
        if (headers != null && !headers.isEmpty())
            headers.forEach(httpHeaders::add);

        // BODY
        Object body = null;
        if (    invocationMethod == RestNodeMetamodel.InvocationMethod.POST ||
                invocationMethod == RestNodeMetamodel.InvocationMethod.PUT ||
                invocationMethod == RestNodeMetamodel.InvocationMethod.PATCH
        )
            body = getBody(context);

        if(body == null) body = new HashMap<>();

        // Execute request
        HttpEntity<?> httpEntity = new HttpEntity<>(body, httpHeaders);
        ResponseEntity<String> response = executeRequest(finalUri.toString(), invocationMethod, httpEntity);

        // Store response in context
        handleOutputPorts(context, response);

        logger.info("[Node {}]: REST request processed successfully.", getId());
    }


    /**
     * Executes the HTTP request using RestTemplate.
     * @param serviceUri The URI of the service to be invoked.
     * @param method The HTTP method to be used (GET, POST, etc.).
     * @param httpEntity The HTTP entity containing the request body and headers.
     * @return The response entity from the service.
     */
    private ResponseEntity<String> executeRequest(String serviceUri, RestNodeMetamodel.InvocationMethod method, HttpEntity<?> httpEntity) {
        HttpMethod httpMethod = convertToHttpMethod(method);
        return restTemplate.exchange(serviceUri, httpMethod, httpEntity, String.class);
    }

    /**
     * Converts the RestNodeMetamodel.InvocationMethod to HttpMethod.
     * @param method The invocation method from the metamodel.
     * @return The corresponding HttpMethod.
     */
    private HttpMethod convertToHttpMethod(RestNodeMetamodel.InvocationMethod method) {
        return switch (method) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case PATCH -> HttpMethod.PATCH;
            case DELETE -> HttpMethod.DELETE;
            case HEAD -> HttpMethod.HEAD;
            case OPTIONS -> HttpMethod.OPTIONS;
        };
    }


    /**
     * Retrieves default headers from the metamodel.
     * @return A map of default headers.
     */
    private Map<String,String> getDefaultHeaders() {
        return this.getMetamodel().getHeaders();
    }

    /**
     * Retrieves headers from the execution context based on input port definitions,
     * merging them with default headers.
     * Handles:
     * - REQ_HEADER (full map)
     * - REQ_HEADER_FIELD (individual fields)
     * - Default headers from the metamodel.
     * Headers from the context/ports override default headers and REQ_HEADER_FIELD
     * overrides keys from REQ_HEADER.
     * @param context The execution context.
     * @return A map of headers.
     */
    private Map<String,String> getHeader(ExecutionContext context) {
        Map<String, String> defaultHeaders = getDefaultHeaders();
        Map<String, String> portHeaders = new HashMap<>();
        Map<String, String> fieldHeaders = new HashMap<>();

        List<RestPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (RestPort inputPort : inputPorts) {
                // HANDLE REQ_HEADER
                if (inputPort.getRole() == RestPort.RestPortRole.REQ_HEADER) {
                    Object headerValue = context.get(inputPort.getKey());
                    if (headerValue instanceof Map) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, String> mapValue = (Map<String, String>) headerValue;
                            portHeaders.putAll(mapValue);
                        } catch (ClassCastException e) {
                            logger.warn("Value for port '{}' with role REQ_HEADER is not a Map<String, String>.", inputPort.getKey(), e);
                        }
                    } else if (headerValue != null) {
                        logger.warn("Value for port '{}' with role REQ_HEADER is not a Map.", inputPort.getKey());
                    }
                }
                // HANDLE REQ_HEADER_FIELD
                else if (inputPort.getRole() == RestPort.RestPortRole.REQ_HEADER_FIELD) {
                    Object fieldValue = context.get(inputPort.getKey());
                    if (fieldValue != null) fieldHeaders.put(inputPort.getKey(), String.valueOf(fieldValue));
                }
            }
        }

        // MERGE HEADERS
        Map<String, String> finalHeaders = new HashMap<>();
        if (defaultHeaders != null) finalHeaders.putAll(defaultHeaders);

        if (!portHeaders.isEmpty()) finalHeaders.putAll(portHeaders);

        // REQ_HEADER_FIELD overrides both defaults and REQ_HEADER
        if (!fieldHeaders.isEmpty()) finalHeaders.putAll(fieldHeaders);

        return finalHeaders.isEmpty() ? null : finalHeaders;
    }


    /**
     * Retrieves query parameters from the execution context based on input port definitions.
     * @param context The execution context.
     * @return A map of query parameters.
     */
    private Map<String,String> getQueryParams(ExecutionContext context) {
        Map<String, String> queryParams = new HashMap<>();
        List<RestPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (RestPort inputPort : inputPorts) {
                if (inputPort.getRole() == RestPort.RestPortRole.REQ_QUERY_PARAMETER) {
                    Object queryValue = context.get(inputPort.getKey());
                    if (queryValue != null)
                        queryParams.put(inputPort.getKey(), toQueryParamString(queryValue));
                }
            }
        }
        return queryParams.isEmpty() ? null : queryParams;
    }


    /**
     * Retrieves path variables from the execution context based on input port definitions.
     * Converts all path variable values to String.
     * @param context The execution context.
     * @return A map of path variables.
     */
    private Map<String,String> getPathVariables(ExecutionContext context) {
        Map<String, String> pathVariables = new HashMap<>();
        List<RestPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            for (RestPort inputPort : inputPorts) {
                if (inputPort.getRole() == RestPort.RestPortRole.REQ_PATH_VARIABLE) {
                    Object pathValue = context.get(inputPort.getKey());
                    if (pathValue != null)
                        pathVariables.put(inputPort.getKey(), String.valueOf(pathValue));
                }
            }
        }
        return pathVariables.isEmpty() ? null : pathVariables;
    }


    /**
     * Retrieves the request body from the execution context based on input port definitions.
     * Handles:
     * - REQ_BODY (base body map)
     * - REQ_BODY_FIELD (fields to be merged into the body map)
     * Merges them into a single Map<String, Object>.
     * Multiple REQ_BODY maps are merged together first, then REQ_BODY_FIELD values
     * are merged into the result, overriding keys if they share the same key.
     * @param context The execution context.
     * @return The request body as a Map<String, Object>, or null if no body inputs are found.
     * @throws IllegalArgumentException if the value for a REQ_BODY port is not a Map.
     */
    private Object getBody(ExecutionContext context) {
        Map<String, Object> body = new HashMap<>();

        List<RestPort> inputPorts = getMetamodel().getInputPorts();
        if (inputPorts != null) {
            // Merge all REQ_BODY maps
            for (RestPort inputPort : inputPorts) {
                if (inputPort.getRole() == RestPort.RestPortRole.REQ_BODY) {
                    Object reqBodyValue = context.get(inputPort.getKey());
                    if (reqBodyValue != null) {
                        if (reqBodyValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapValue = (Map<String, Object>) reqBodyValue;
                            body.putAll(mapValue);
                        } else {
                            throw new IllegalArgumentException("Value for port '" + inputPort.getKey() + "' with role REQ_BODY must be a Map.");
                        }
                    }
                }
            }

            // Merge all REQ_BODY_FIELD
            for (RestPort inputPort : inputPorts) {
                if (inputPort.getRole() == RestPort.RestPortRole.REQ_BODY_FIELD) {
                    Object fieldValue = context.get(inputPort.getKey());
                    body.put(inputPort.getKey(), fieldValue);
                }
            }
        }

        return body.isEmpty() ? null : body;
    }



    /**
     * Converts the value to a query parameter string.
     * @param value the value to convert
     * @return the query parameter string
     */
    private String toQueryParamString(Object value) {
        if (value == null) return "";

        String stringValue;
        if (value instanceof LocalDate) {
            stringValue = ((LocalDate) value).format(DateTimeFormatter.ISO_DATE);
        } else if (value instanceof Enum) {
            stringValue = ((Enum<?>) value).name();
        }
        else {
            stringValue = String.valueOf(value);
        }

        try {
            return URLEncoder.encode(stringValue, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error encoding query parameter value: " + value, e);
        }
    }

    /**
     * Handles the output ports, writing response data to the execution context
     * based on the output port roles (RES_FULL_BODY, RES_BODY_FIELD, RES_STATUS, RES_HEADERS).
     * Includes parsing of the response body for RES_BODY_FIELD.
     * @param context The execution context.
     * @param response The ResponseEntity received from the REST call.
     */
    private void handleOutputPorts(ExecutionContext context, ResponseEntity<String> response) {
        List<RestPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts == null || outputPorts.isEmpty()) {
            logger.info("Node {}: No output ports defined.", getId());
            return;
        }

        String responseBody = response.getBody();
        HttpHeaders responseHeaders = response.getHeaders();
        int responseStatus = response.getStatusCode().value();

        Map<String, Object> parsedResponseBody = null;
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            try {
                // Parse the body as a JSON
                parsedResponseBody = objectMapper.readValue(responseBody, new TypeReference<>() {});
                logger.debug("[Node {}]: Successfully parsed response body.", getId());
            } catch (Exception e) {
                logger.warn("[Node {}]: Could not parse response body for field extraction: {}", getId(), e.getMessage());
                parsedResponseBody = null;
            }
        }


        for (RestPort outputPort : outputPorts) {
            if(outputPort == null || outputPort.getRole() == null) continue;

            Object valueToSet = null;
            switch (outputPort.getRole()) {
                case RES_FULL_BODY:
                    if(outputPort.getSchema().getType() == PortType.OBJECT && parsedResponseBody != null ){
                        valueToSet = parsedResponseBody;
                        logger.debug("[Node {}]: Handling RES_FULL_BODY for port '{}' with parsed body.", getId(), outputPort.getKey());
                    }
                    else valueToSet = responseBody;

                    logger.debug("[Node {}]: Handling RES_FULL_BODY for port '{}'.", getId(), outputPort.getKey());

                    break;
                case RES_STATUS:
                    valueToSet = responseStatus;
                    logger.debug("[Node {}]: Handling RES_STATUS for port '{}'.", getId(), outputPort.getKey());
                    break;
                case RES_HEADERS:
                    logger.debug("[Node {}]: Handling RES_HEADERS for port '{}'.", getId(), outputPort.getKey());
                    if (responseHeaders != null) {
                        valueToSet = responseHeaders.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Entry::getKey,
                                        entry -> {
                                            List<String> values = entry.getValue();
                                            return (values == null || values.isEmpty()) ? "" : String.join(",", values);
                                        }
                                ));
                    } else {
                        logger.debug("[Node {}]: Response headers are null.", getId());
                    }
                    break;
                case RES_BODY_FIELD:
                    logger.debug("[Node {}]: Handling RES_BODY_FIELD for port '{}'.", getId(), outputPort.getKey());
                    if (parsedResponseBody != null) {
                        // Use the port key to extract the field from the parsed body
                        valueToSet = parsedResponseBody.get(outputPort.getKey());
                        logger.debug("[Node {}]: Extracted body field '{}' with value: {}.", getId(), outputPort.getKey(), valueToSet);
                    } else {
                        if (responseBody == null || responseBody.trim().isEmpty())
                            logger.debug("[Node {}]: Cannot extract body field '{}' because response body is null or empty.", getId(), outputPort.getKey());
                        else
                            logger.warn("[Node {}]: Cannot extract body field '{}' because response body was not successfully parsed into a Map-like structure.", getId(), outputPort.getKey());
                    }
                    break;

                default:
                    logger.warn("[Node {}]: Output port '{}' has unexpected role: {}. This port will be ignored.", getId(), outputPort.getKey(), outputPort.getRole());
                    break;
            }

            context.put(outputPort.getKey(), valueToSet);
            logger.info("[Node {}]: Set output port '{}' with value: {}.", getId(), outputPort.getKey(), valueToSet);
        }
    }
}
