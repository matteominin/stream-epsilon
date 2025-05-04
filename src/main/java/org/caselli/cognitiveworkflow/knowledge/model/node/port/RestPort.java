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
        REQ_BODY_FIELD,
        REQ_BODY,
        REQ_HEADER,
        REQ_HEADER_FIELD,
        REQ_PATH_VARIABLE,
        REQ_QUERY_PARAMETER,
        RES_FULL_BODY,
        RES_BODY_FIELD,
        RES_STATUS,
        RES_HEADERS,
    }
}
