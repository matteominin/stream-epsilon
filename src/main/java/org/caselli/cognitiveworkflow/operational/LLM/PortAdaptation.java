package org.caselli.cognitiveworkflow.operational.LLM;


import lombok.Data;

import java.util.Map;

@Data
public class PortAdaptation {

    private Map<String, String> bindings;

    @Override
    public String toString() {
        return "PortAdaptation{" +
                "binding=" + bindings +
                '}';
    }
}