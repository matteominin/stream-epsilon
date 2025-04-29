package org.caselli.cognitiveworkflow.knowledge.model.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.Map;


/**
 * Defines the data type and structure of a port, supporting nested schemas for objects and arrays.
 */
@Data
public class PortSchema {
    /** Basic data type of the port (e.g. STRING, BOOLEAN, etc.) */
    private PortType type;

    /** Schema for items if the port is an array */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PortSchema items;

    /** Schema for properties if the port is an object */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PortSchema> properties;

    /** Default value for the port */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object defaultValue;

    /** Whether the port is required for execution */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean required;

    /**
     * Checks if this schema is compatible with another schema for data flow.
     * @param target The target schema to check compatibility with
     * @return true if the schemas are compatible, false otherwise
     */
    public static boolean isCompatible(PortSchema source, PortSchema target) {
        if (source == null || target == null) return false;

        if (!isTypeCompatible(source.type, target.type)) return false;

        // ARRAYS
        if (source.type == PortType.ARRAY) {
            if (target.type != PortType.ARRAY) return false;
            // Check array item types
            if (!isCompatible(source.items, target.items)) return false;
        }

        // OBJECTS
        if (source.type == PortType.OBJECT) {
            if (target.type != PortType.OBJECT) return false;

            // If target has no properties defined, it accepts any object
            if (target.properties == null || target.properties.isEmpty()) return true;

            // If source has no properties, it can't satisfy target's properties
            if (source.properties == null || source.properties.isEmpty()) return false;

            // Check that all required properties in target exist in source and are compatible
            for (Map.Entry<String, PortSchema> targetProp : target.properties.entrySet()) {
                PortSchema sourcePropSchema = source.properties.get(targetProp.getKey());
                if (sourcePropSchema == null) return false;
                if (!isCompatible(sourcePropSchema, targetProp.getValue()))  return false;
            }
        }

        return true;
    }

    /**
     * Checks if source type can be safely converted to target type
     * @param source The source type
     * @param target The target type
     */
    private static boolean isTypeCompatible(PortType source, PortType target) {
        if (source == target) return true;

        // INT can be converted to FLOAT
        if (source == PortType.INT && target == PortType.FLOAT)
            return true;


        // FLOAT can sometimes be converted to INT (with potential loss of precision)
        if (source == PortType.FLOAT && target == PortType.INT)
            return true;

        return false;
    }

    /**
     * Checks if this schema is compatible with another schema for data flow.
     * @param target The target schema to check compatibility with
     * @return true if the schemas are compatible, false otherwise
     */
    public boolean isCompatibleWith(PortSchema target) {
        return isCompatible(this, target);
    }
}