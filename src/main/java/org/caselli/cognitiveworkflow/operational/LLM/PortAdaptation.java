package org.caselli.cognitiveworkflow.operational.LLM;

import lombok.Data;
import java.util.Map;

/**
 * Class representing the adaptation of ports.
 * It contains a map of bindings that represent the adaptation between source and target ports.
 * Use dot notation for nested attributes (e.g. "A.B").
 */
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