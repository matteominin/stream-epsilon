package org.caselli.cognitiveworkflow.knowledge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class Port {
    @NotNull
    private String key;

    @NotNull
    private PortSchema schema;

    @Data
    public static class PortSchema {
        private PortType type;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private PortSchema items;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, PortSchema> properties;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object defaultValue;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Boolean required;
    }

    public enum PortType {
        STRING,
        INT,
        FLOAT,
        BOOLEAN,
        DATE,
        OBJECT,
        ARRAY
    }
}