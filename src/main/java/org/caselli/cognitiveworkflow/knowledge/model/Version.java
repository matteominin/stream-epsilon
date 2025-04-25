package org.caselli.cognitiveworkflow.knowledge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Version {
    @NotNull
    @JsonProperty("major")
    private int major;

    @NotNull
    @JsonProperty("minor")
    private int minor;

    @NotNull
    @JsonProperty("patch")
    private int patch;

    @JsonProperty("label")
    private String label;

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (label != null ? "-" + label : "");
    }
}