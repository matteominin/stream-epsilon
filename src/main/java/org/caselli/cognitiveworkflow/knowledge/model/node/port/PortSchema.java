package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
     * Converts a PortSchema to JSON string representation.
     * @return JSON string representation of the PortSchema object
     */
    public String toJson(){
        ObjectMapper mapper = new ObjectMapper();
        // Exclude null fields
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
            JsonNode portNode = mapper.valueToTree(this);
            return mapper.writeValueAsString(portNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize PortSchema to JSON", e);
        }
    }


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
        if(value == null || value.toString().isEmpty()) return !schema.getRequired();

        PortType schemaType = schema.getType();

        switch (schemaType) {
            case STRING:
                return (value instanceof String);

            case INT:
                return (value instanceof Integer) || (value instanceof Long);

            case FLOAT:
                return (value instanceof Float) || (value instanceof Double);

            case BOOLEAN:
                return (value instanceof Boolean);

            case DATE:
                return (value instanceof Date);

            case ARRAY:
                if (!(value instanceof List<?> || value instanceof Object[]))
                    return false;

                PortSchema itemSchema = schema.getItems();
                if (itemSchema == null) return false;

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

                        if (propSchema == null) return true;

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
     * Resolves a dot-notated path to a nested PortSchema, starting from this schema.
     * Example: if this schema is an object with a property "user" which is an object
     * with a property "name", then calling getSchemaByPath("user.name") on the parent schema
     * would return the schema for "name".
     * @param path The dot-notated path string
     * @return The PortSchema at the end of the path. If path is null/empty, returns this schema.
     * @throws IllegalArgumentException if the path is invalid
     */
    public PortSchema getSchemaByPath(String path) {
        if (path == null || path.isEmpty()) return this;

        String[] parts = path.split("\\.");
        PortSchema currentSchema = this;

        for (int i = 0; i < parts.length; i++) {
            String propertyName = parts[i];
            if (propertyName.isEmpty()) {
                String walkedPath = (i > 0) ? String.join(".", Arrays.copyOfRange(parts, 0, i)) : "root";
                throw new IllegalArgumentException("Path segment cannot be empty at segment " + (i+1) + " (after '" + walkedPath + "') in full path: '" + path + "'.");
            }

            if (currentSchema.getType() != PortType.OBJECT) {
                String walkedPath = (i > 0) ? String.join(".", Arrays.copyOfRange(parts, 0, i)) : "root";
                throw new IllegalArgumentException("Cannot access property '" + propertyName + "' on type " + currentSchema.getType() + ". Schema at '" + walkedPath + "' is not an OBJECT. Full path: '" + path + "'.");
            }

            if (currentSchema.getProperties() == null) {
                String walkedPath = (i > 0) ? String.join(".", Arrays.copyOfRange(parts, 0, i)) : "root";
                throw new IllegalArgumentException("Cannot access property '" + propertyName + "' because parent object schema at '" + walkedPath + "' has no properties defined. Full path: '" + path + "'.");
            }

            PortSchema nextSchema = currentSchema.getProperties().get(propertyName);
            if (nextSchema == null) {
                String walkedPath = (i > 0) ? String.join(".", Arrays.copyOfRange(parts, 0, i)) : "root";
                throw new IllegalArgumentException("Property '" + propertyName + "' not found in object schema at '" + walkedPath + "'. Full path: '" + path + "'. Available properties: " + currentSchema.getProperties().keySet());
            }

            currentSchema = nextSchema;
        }
        return currentSchema;
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