package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class RestPort extends Port {

    /** Defines the role this port plays in REST context */
    @NotNull private RestPortRole role;

    public RestPort() {
        this.setPortType(PortImplementationType.REST);
    }

    /**
     * REST-specific port roles
     */
    public enum RestPortRole {
        REQUEST_BODY,
        PATH_VARIABLE,
        QUERY_PARAMETER,
        HEADER,
        RESPONSE_BODY,
        RESPONSE_STATUS,
        RESPONSE_HEADERS
    }
}
