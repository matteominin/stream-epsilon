package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RestPort extends Port {

    /** Defines the role this port plays in REST context */
    private RestPortRole role;

    public static RestPortBuilder builder() {
        return new RestPortBuilder();
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

    /**
     * Rest Tool Port builder
     */
    public static class RestPortBuilder extends AbstractPortBuilder<RestPort, RestPortBuilder> {
        private RestPortRole role;

        public RestPortBuilder withRole(RestPortRole role) {
            this.role = role;
            return this;
        }

        @Override
        protected RestPortBuilder self() {
            return this;
        }

        @Override
        protected RestPort createInstance() {
            var port = new RestPort();
            port.setRole(role);
            port.setPortType(PortImplementationType.REST);
            return port;
        }
    }
}
