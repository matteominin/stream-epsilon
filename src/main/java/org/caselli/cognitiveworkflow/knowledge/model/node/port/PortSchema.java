package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.*;


/**
 * Defines the data type and structure of a port, supporting nested schemas for objects and arrays.
 */
@Data
public class PortSchema {


    PortSchema(){}
    public static PortSchemaBuilder builder() {
        return new PortSchemaBuilder();
    }


    /** Basic data type of the port (e.g. STRING, BOOLEAN, etc.) */
    private PortType type;

    /** Schema for items if the port is an array */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PortSchema items;

    /** Schema for properties if the port is an object */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, PortSchema> properties;


    /**
     * Whether the port is required or not.
     * NOTE: This is only for input ports, as output ports are always required.
     * TODO:gestire diversamente
     * */
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
        return source == PortType.FLOAT && target == PortType.INT;
    }

    /**
     * Checks if this schema is compatible with another schema for data flow.
     * @param target The target schema to check compatibility with
     * @return true if the schemas are compatible, false otherwise
     */
    public boolean isCompatibleWith(PortSchema target) {
        return isCompatible(this, target);
    }

    /**
     * Validates if the given value is compliant with this schema.
     * @param value The value to validate
     * @return true if the value is valid according to the schema, false otherwise.
     */
    public boolean isValidValue(Object value) {
        return isValidValue(value, this);
    }


    /**
     * Method to validate a value against a schema
     * @param value The current segment of the value being validated.
     * @param schema The PortSchema segment to validate against.
     * @return true if the value segment is valid according to the schema segment
     */
    public static boolean isValidValue(Object value, PortSchema schema) {
        if (schema == null || schema.getType() == null) return value == null;
        if(value == null) return !schema.getRequired();

        PortType schemaType = schema.getType();

        switch (schemaType) {
            case STRING:
                return value instanceof String;

            case INT:
                return value instanceof Integer || value instanceof Long;

            case FLOAT:
                return value instanceof Float || value instanceof Double;

            case BOOLEAN:
                return value instanceof Boolean;

            case DATE:
                return value instanceof Date;

            case ARRAY:
                if (!(value instanceof List<?> || value instanceof Object[]))
                    return false;

                PortSchema itemSchema = schema.getItems();
                if (itemSchema == null) return true;

                if (value instanceof List<?> array)
                    for (Object item : array) {
                        if (!isValidValue(item, itemSchema))
                            return false;
                } else {
                    Object[] array = (Object[]) value;
                    for (Object item : array)
                        if (!isValidValue(item, itemSchema)) return false;
                }
                return true;

            case OBJECT:
                if (!(value instanceof Map<?, ?> || value instanceof org.bson.Document)) return false;

                Map<?, ?> objectMap;
                if (value instanceof Map)
                    objectMap = (Map<?, ?>) value;
                else
                    objectMap = ((org.bson.Document) value);


                Map<String, PortSchema> propertiesSchema = schema.getProperties();
                List<String> checkedProperties = new ArrayList<>();

                if (propertiesSchema != null) {
                    for (Map.Entry<?, ?> entry : objectMap.entrySet()) {
                        Object propKey = entry.getKey();
                        Object propValue = entry.getValue();

                        if (!(propKey instanceof String propName)) return false;

                        PortSchema propSchema = propertiesSchema.get(propName);

                        if (propSchema == null) return false;

                        if (!isValidValue(propValue, propSchema)) return false;

                        checkedProperties.add(propName);
                    }

                    // Check if some required properties are missing
                    for (Map.Entry<String, PortSchema> entry : propertiesSchema.entrySet()) {
                        String propName = entry.getKey();
                        PortSchema propSchema = entry.getValue();

                        if (propSchema.getRequired() != null && propSchema.getRequired() && !checkedProperties.contains(propName))
                            return false;
                    }
                }


                return true;

            default:
                return false;
        }
    }


    /**
     * Builder
     */
    public static class PortSchemaBuilder {
        private PortType type;
        private PortSchema items;
        private Map<String, PortSchema> properties;
        private Boolean required;

        private PortSchemaBuilder withType(PortType type) {
            this.type = type;
            return this;
        }

        private PortSchemaBuilder withItems(PortSchema items) {
            this.items = items;
            return this;
        }

        private PortSchemaBuilder withProperty(String name, PortSchema propertySchema) {
            if (this.properties == null) {
                this.properties = new HashMap<>();
            }
            this.properties.put(name, propertySchema);
            return this;
        }

        private PortSchemaBuilder withProperties(Map<String, PortSchema> properties) {
            this.properties = properties;
            return this;
        }

        public PortSchemaBuilder withRequired(Boolean required) {
            this.required = required;
            return this;
        }

        public PortSchema build() {
            PortSchema schema = new PortSchema();
            schema.setType(type);
            schema.setItems(items);
            schema.setProperties(properties);
            schema.setRequired(required);


            if(type == null)
                throw new IllegalArgumentException("PortSchema type cannot be null");

            if(type == PortType.OBJECT && properties == null)
                throw new IllegalArgumentException("PortSchema type is OBJECT but properties are null");

            if(type == PortType.ARRAY && items == null)
                throw new IllegalArgumentException("PortSchema type is ARRAY but items are null");

            if(type != PortType.ARRAY && items != null)
                throw new IllegalArgumentException("PortSchema type is not ARRAY but items are not null");

            if(type != PortType.OBJECT && properties != null)
                throw new IllegalArgumentException("PortSchema type is not OBJECT but properties are not null");


            return schema;
        }

        public PortSchemaBuilder stringSchema() {
            return withType(PortType.STRING);
        }

        public PortSchemaBuilder intSchema() {
            return withType(PortType.INT);
        }

        public PortSchemaBuilder floatSchema() {
            return withType(PortType.FLOAT);
        }

        public PortSchemaBuilder booleanSchema() {
            return withType(PortType.BOOLEAN);
        }

        public PortSchemaBuilder dateSchema() {
            return withType(PortType.DATE);
        }

        public PortSchemaBuilder arraySchema(PortSchema itemsSchema) {
            return withType(PortType.ARRAY).withItems(itemsSchema);
        }

        public PortSchemaBuilder objectSchema(Map<String, PortSchema> properties) {
            return withType(PortType.OBJECT).withProperties(properties);
        }
    }
}