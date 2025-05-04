package org.caselli.cognitiveworkflow.operational.node;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.port.RestPort;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RestToolNodeInstance extends ToolNodeInstance {

    private static final Logger logger = LoggerFactory.getLogger(ToolNodeInstance.class);

    private RestTemplate restTemplate;

    public RestToolNodeInstance() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public RestToolNodeMetamodel getMetamodel() {
        return (RestToolNodeMetamodel) super.getMetamodel();
    }

    @Override
    public void setMetamodel(NodeMetamodel metamodel) {
        if (!(metamodel instanceof RestToolNodeMetamodel)) {
            throw new IllegalArgumentException("RestToolNodeInstance requires RestToolNodeMetamodel");
        }
        super.setMetamodel(metamodel);
    }

    @Override
    public void process(ExecutionContext context) throws Exception {
        System.out.println("Processing Rest Tool Node Instance: " + getId());

        RestToolNodeMetamodel metamodel = getMetamodel();
        String serviceUri = metamodel.getServiceUri();
        RestToolNodeMetamodel.InvocationMethod invocationMethod = metamodel.getInvocationMethod();

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
        Object requestBody = new Object();
        if (    invocationMethod == RestToolNodeMetamodel.InvocationMethod.POST ||
                invocationMethod == RestToolNodeMetamodel.InvocationMethod.PUT ||
                invocationMethod == RestToolNodeMetamodel.InvocationMethod.PATCH
        )
            requestBody = getBody(context);


        HttpEntity<?> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
        ResponseEntity<String> response = executeRequest(finalUri.toString(), invocationMethod, httpEntity);

        // Store response in context (using output ports as per the clarified design)
        handleOutputPorts(context, response);


        System.out.println("Rest Tool Node Instance processed successfully: " + getId());
    }


    /**
     * Executes the HTTP request using RestTemplate.
     * @param serviceUri The URI of the service to be invoked.
     * @param method The HTTP method to be used (GET, POST, etc.).
     * @param httpEntity The HTTP entity containing the request body and headers.
     * @return The response entity from the service.
     */
    private ResponseEntity<String> executeRequest(String serviceUri, RestToolNodeMetamodel.InvocationMethod method, HttpEntity<?> httpEntity) {
        HttpMethod httpMethod = convertToHttpMethod(method);
        return restTemplate.exchange(serviceUri, httpMethod, httpEntity, String.class);
    }

    /**
     * Converts the RestToolNodeMetamodel.InvocationMethod to HttpMethod.
     * @param method The invocation method from the metamodel.
     * @return The corresponding HttpMethod.
     */
    private HttpMethod convertToHttpMethod(RestToolNodeMetamodel.InvocationMethod method) {
        return switch (method) {
            case GET -> HttpMethod.GET;
            case POST -> HttpMethod.POST;
            case PUT -> HttpMethod.PUT;
            case PATCH -> HttpMethod.PATCH;
            case DELETE -> HttpMethod.DELETE;
            case HEAD -> HttpMethod.HEAD;
            case OPTIONS -> HttpMethod.OPTIONS;
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
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
        Map<String, String> defaultHeaders = getMetamodel().getHeaders();
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
                            System.err.println("Warning: Value for port '" + inputPort.getKey() + "' with role REQ_HEADER is not a Map<String, String>.");
                        }
                    } else if (headerValue != null) {
                        System.err.println("Warning: Value for port '" + inputPort.getKey() + "' with role REQ_HEADER is not a Map.");
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
     * based on the output port roles.
     * @param context The execution context.
     * @param response The ResponseEntity received from the REST call.
     */
    private void handleOutputPorts(ExecutionContext context, ResponseEntity<String> response) {
        List<RestPort> outputPorts = getMetamodel().getOutputPorts();
        if (outputPorts == null || outputPorts.isEmpty()) {
            return;
        }

        for (RestPort outputPort : outputPorts) {
            Object valueToSet = null;
            switch (outputPort.getRole()) {
                case RESPONSE_BODY:
                    valueToSet = response.getBody();
                    break;
                case RESPONSE_STATUS:
                    valueToSet = response.getStatusCode().value();
                    break;
                case RESPONSE_HEADERS:
                    // Convert HttpHeaders to Map<String, String>
                    HttpHeaders responseHeaders = response.getHeaders();
                    if (responseHeaders != null) {
                        valueToSet = responseHeaders.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Entry::getKey,
                                        entry -> {
                                            List<String> values = entry.getValue();
                                            return (values == null || values.isEmpty()) ? "" : String.join(",", values);
                                        }
                                ));
                    }
                    break;

                default:
                    System.err.println("Warning: Unexpected output port role: " + outputPort.getRole());
                    break;
            }

            if (valueToSet != null) {
                context.put(outputPort.getKey(), valueToSet);
                System.out.println("Set output port '" + outputPort.getKey() + "' with value: " + valueToSet);
            } else {
                context.put(outputPort.getKey(), null);
                System.out.println("Set output port '" + outputPort.getKey() + "' with null value.");
            }
        }
    }

}