package org.caselli.cognitiveworkflow.operational.node;

import lombok.Getter;
import lombok.Setter;
import org.caselli.cognitiveworkflow.knowledge.model.node.NodeMetamodel;
import org.caselli.cognitiveworkflow.knowledge.model.node.RestToolNodeMetamodel;
import org.caselli.cognitiveworkflow.operational.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Component
@Scope("prototype")
public class RestToolNodeInstance extends ToolNodeInstance {

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
        Map<String, String> headers = metamodel.getHeaders();

        // Path variables (e.g., /resource/{id})
        @SuppressWarnings("unchecked")
        Map<String, Object> pathVariables = (Map<String, Object>) context.get("pathVariables");

        if (pathVariables != null && !pathVariables.isEmpty()) {
            for (Map.Entry<String, Object> entry : pathVariables.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                serviceUri = serviceUri.replace(placeholder, value);
            }
        }

        // Query parameters (e.g., ?param1=value1&param2=value2)
        @SuppressWarnings("unchecked")
        Map<String, String> queryParams = (Map<String, String>) context.get("queryParams");
        if (queryParams == null) queryParams = new HashMap<>();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(serviceUri);
        if (!queryParams.isEmpty()) queryParams.forEach(uriBuilder::queryParam);

        URI finalUri = uriBuilder.build().toUri();

        // Headers
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null && !headers.isEmpty())
            headers.forEach(httpHeaders::add);


        // Body
        Object requestBody = new Object();

        if (    invocationMethod == RestToolNodeMetamodel.InvocationMethod.POST ||
                invocationMethod == RestToolNodeMetamodel.InvocationMethod.PUT ||
                invocationMethod == RestToolNodeMetamodel.InvocationMethod.PATCH
            )
            requestBody = context.get("requestBody");


        HttpEntity<?> httpEntity = new HttpEntity<>(requestBody, httpHeaders);
        ResponseEntity<String> response = executeRequest(finalUri.toString(), invocationMethod, httpEntity);

        // Store response in context
      /*  context.getVariables().put("responseBody", response.getBody());
        context.getVariables().put("responseStatus", response.getStatusCode().value());
        context.getVariables().put("responseHeaders", response.getHeaders());
        &TODO
       */

        System.out.println("Rest Tool Node Instance processed successfully: " + getId());
    }


    private ResponseEntity<String> executeRequest(String serviceUri, RestToolNodeMetamodel.InvocationMethod method, HttpEntity<?> httpEntity) {
        HttpMethod httpMethod = convertToHttpMethod(method);
        return restTemplate.exchange(serviceUri, httpMethod, httpEntity, String.class);
    }

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
}