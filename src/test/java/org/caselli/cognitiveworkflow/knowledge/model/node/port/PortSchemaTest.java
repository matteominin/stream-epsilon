package org.caselli.cognitiveworkflow.knowledge.model.node.port;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.bson.Document;

@Tag("test")
class PortSchemaTest {

    @Test
    void testBuilderCreation() {
        PortSchema schema = PortSchema.builder().stringSchema().build();
        assertEquals(PortType.STRING, schema.getType());
    }

    @Test
    void testArraySchemaCreation() {
        PortSchema itemSchema = PortSchema.builder().intSchema().build();
        PortSchema arraySchema = PortSchema.builder().arraySchema(itemSchema).build();

        assertEquals(PortType.ARRAY, arraySchema.getType());
        assertNotNull(arraySchema.getItems());
        assertEquals(PortType.INT, arraySchema.getItems().getType());
    }

    @Test
    void testObjectSchemaCreation() {
        Map<String, PortSchema> properties = new HashMap<>();
        properties.put("name", PortSchema.builder().stringSchema().build());
        properties.put("age", PortSchema.builder().intSchema().build());

        PortSchema objectSchema = PortSchema.builder().objectSchema(properties).build();

        assertEquals(PortType.OBJECT, objectSchema.getType());
        assertNotNull(objectSchema.getProperties());
        assertEquals(2, objectSchema.getProperties().size());
    }

    @Test
    void testSchemaCompatibility() {
        PortSchema stringSchema = PortSchema.builder().stringSchema().build();
        PortSchema intSchema = PortSchema.builder().intSchema().build();
        PortSchema floatSchema = PortSchema.builder().floatSchema().build();

        // Same types
        assertTrue(PortSchema.isCompatible(stringSchema, stringSchema));
        assertTrue(PortSchema.isCompatible(intSchema, intSchema));

        // Numeric compatibility
        assertTrue(PortSchema.isCompatible(intSchema, floatSchema));
        assertTrue(PortSchema.isCompatible(floatSchema, intSchema));

        // Incompatible types
        assertFalse(PortSchema.isCompatible(stringSchema, intSchema));

        // Null cases
        assertFalse(PortSchema.isCompatible(null, stringSchema));
        assertFalse(PortSchema.isCompatible(stringSchema, null));
    }

    @Test
    void testArrayCompatibility() {
        PortSchema intArraySchema = PortSchema.builder()
                .arraySchema(PortSchema.builder().intSchema().build())
                .build();

        PortSchema floatArraySchema = PortSchema.builder()
                .arraySchema(PortSchema.builder().floatSchema().build())
                .build();

        PortSchema stringArraySchema = PortSchema.builder()
                .arraySchema(PortSchema.builder().stringSchema().build())
                .build();

        // Compatible arrays (int -> float)
        assertTrue(PortSchema.isCompatible(intArraySchema, floatArraySchema));

        // Incompatible arrays
        assertFalse(PortSchema.isCompatible(intArraySchema, stringArraySchema));

        // Array vs non-array
        assertFalse(PortSchema.isCompatible(intArraySchema, PortSchema.builder().intSchema().build()));
    }

    @Test
    void testObjectCompatibility() {
        Map<String, PortSchema> personProps = new HashMap<>();
        personProps.put("name", PortSchema.builder().stringSchema().build());
        personProps.put("age", PortSchema.builder().intSchema().build());

        Map<String, PortSchema> personPropsWithAddress = new HashMap<>();
        personPropsWithAddress.put("name", PortSchema.builder().stringSchema().build());
        personPropsWithAddress.put("age", PortSchema.builder().intSchema().build());
        personPropsWithAddress.put("address", PortSchema.builder().stringSchema().build());

        PortSchema personSchema = PortSchema.builder().objectSchema(personProps).build();
        PortSchema personWithAddressSchema = PortSchema.builder().objectSchema(personPropsWithAddress).build();
        PortSchema openObjectSchema = PortSchema.builder().objectSchema(new HashMap<>()).build();

        // Same schema
        assertTrue(PortSchema.isCompatible(personSchema, personSchema));

        // Target has fewer properties
        assertTrue(PortSchema.isCompatible(personWithAddressSchema, personSchema));

        // Target has more properties
        assertFalse(PortSchema.isCompatible(personSchema, personWithAddressSchema));

        // Open object schema accepts any object
        assertTrue(PortSchema.isCompatible(personSchema, openObjectSchema));
        assertTrue(PortSchema.isCompatible(personWithAddressSchema, openObjectSchema));

        // Empty source object
        PortSchema emptyObjectSchema = PortSchema.builder().objectSchema(new HashMap<>()).build();
        assertFalse(PortSchema.isCompatible(emptyObjectSchema, personSchema));
    }

    @Test
    void testValidValuePrimitives() {
        PortSchema stringSchema = PortSchema.builder().stringSchema().build();
        PortSchema intSchema = PortSchema.builder().intSchema().build();
        PortSchema floatSchema = PortSchema.builder().floatSchema().build();
        PortSchema booleanSchema = PortSchema.builder().booleanSchema().build();

        // Valid values
        String testString = "test";
        assertTrue(stringSchema.isValidValue(testString));
        assertTrue(intSchema.isValidValue(42));
        assertTrue(floatSchema.isValidValue(3.14f));
        assertTrue(booleanSchema.isValidValue(true));

        // Invalid values
        assertFalse(stringSchema.isValidValue(42));
        assertFalse(intSchema.isValidValue("42"));
        assertFalse(floatSchema.isValidValue(true));
        assertFalse(booleanSchema.isValidValue(1));
    }

    @Test
    void testValidValueWithRequired() {
        PortSchema requiredSchema = PortSchema.builder().stringSchema().withRequired(true).build();
        PortSchema optionalSchema = PortSchema.builder().stringSchema().withRequired(false).build();

        assertFalse(requiredSchema.isValidValue(null));
        assertTrue(optionalSchema.isValidValue(null));
        assertTrue(requiredSchema.isValidValue("present"));
    }

    @Test
    void testValidValueArrays() {
        PortSchema intArraySchema = PortSchema.builder()
                .arraySchema(PortSchema.builder().intSchema().build())
                .build();

        // Valid array
        assertTrue(intArraySchema.isValidValue(new Integer[]{1, 2, 3}));
        assertTrue(intArraySchema.isValidValue(Arrays.asList(1, 2, 3)));

        // Invalid array (wrong item type)
        assertFalse(intArraySchema.isValidValue(new String[]{"a", "b", "c"}));
        assertFalse(intArraySchema.isValidValue(Arrays.asList("a", "b", "c")));

        // Non-array value
        assertFalse(intArraySchema.isValidValue(42));
    }

    @Test
    void testValidValueObjects() {
        Map<String, PortSchema> personProps = new HashMap<>();
        personProps.put("name", PortSchema.builder().stringSchema().withRequired(true).build());
        personProps.put("age", PortSchema.builder().intSchema().withRequired(false).build());

        PortSchema personSchema = PortSchema.builder().objectSchema(personProps).build();

        // Valid object (Map)
        Map<String, Object> validPersonMap = new HashMap<>();
        validPersonMap.put("name", "John");
        validPersonMap.put("age", 30);
        assertTrue(personSchema.isValidValue(validPersonMap));

        // Valid object (Document)
        Document validPersonDoc = new Document("name", "John").append("age", 30);
        assertTrue(personSchema.isValidValue(validPersonDoc));

        // Missing required field
        Map<String, Object> missingName = new HashMap<>();
        missingName.put("age", 30);
        assertFalse(personSchema.isValidValue(missingName));

        // Wrong field type
        Map<String, Object> wrongType = new HashMap<>();
        wrongType.put("name", 123); // should be string
        wrongType.put("age", 30);
        assertFalse(personSchema.isValidValue(wrongType));

        // Extra fields (should be allowed)
        Map<String, Object> extraFields = new HashMap<>();
        extraFields.put("name", "John");
        extraFields.put("age", 30);
        extraFields.put("address", "123 Main St");
        assertTrue(personSchema.isValidValue(extraFields));
    }

    @Test
    void testIsCompatibleWithMethod() {
        PortSchema source = PortSchema.builder().intSchema().build();
        PortSchema target = PortSchema.builder().floatSchema().build();

        assertTrue(source.isCompatibleWith(target));
        assertTrue(target.isCompatibleWith(source));
    }

    @Test
    void testGetSchemaByPath() {
        Map<String, PortSchema> addressProps = new HashMap<>();
        addressProps.put("street", PortSchema.builder().stringSchema().build());
        addressProps.put("zip", PortSchema.builder().intSchema().build());
        PortSchema addressSchema = PortSchema.builder().objectSchema(addressProps).build();

        Map<String, PortSchema> personProps = new HashMap<>();
        personProps.put("name", PortSchema.builder().stringSchema().build());
        personProps.put("address", addressSchema);

        PortSchema personSchema = PortSchema.builder().objectSchema(personProps).build();

        // Root schema
        assertEquals(personSchema, personSchema.getSchemaByPath(""));

        // Top-level property
        assertEquals(PortType.STRING, personSchema.getSchemaByPath("name").getType());

        // Nested property
        assertEquals(PortType.INT, personSchema.getSchemaByPath("address.zip").getType());

        // Edge case
        assertEquals(addressSchema,  personSchema.getSchemaByPath("address."));

        // Invalid paths
        assertThrows(IllegalArgumentException.class, () -> personSchema.getSchemaByPath("nonexistent"));
        assertThrows(IllegalArgumentException.class, () -> personSchema.getSchemaByPath("name.street"));
        assertThrows(IllegalArgumentException.class, () -> personSchema.getSchemaByPath(".address"));
    }

    @Test
    void testMapToSchemaPrimitives() {
        // String schema
        PortSchema stringSchema = PortSchema.builder().stringSchema().build();
        assertEquals("42", PortSchema.mapToSchema(42, stringSchema));
        assertEquals("true", PortSchema.mapToSchema(true, stringSchema));
        assertEquals("test", PortSchema.mapToSchema("test", stringSchema));

        // Int schema
        PortSchema intSchema = PortSchema.builder().intSchema().build();
        assertEquals(42, PortSchema.mapToSchema(42, intSchema));
        assertEquals(42, PortSchema.mapToSchema("42", intSchema));
        assertEquals(42, PortSchema.mapToSchema(42.0f, intSchema));
        assertNull(PortSchema.mapToSchema("not a number", intSchema));

        // Float schema
        PortSchema floatSchema = PortSchema.builder().floatSchema().build();
        assertEquals(42.0, PortSchema.mapToSchema(42, floatSchema));
        assertEquals(3.14, PortSchema.mapToSchema("3.14", floatSchema));
        assertEquals(3.14, PortSchema.mapToSchema(3.14, floatSchema));
        assertNull(PortSchema.mapToSchema("not a number", floatSchema));

        // Boolean schema
        PortSchema boolSchema = PortSchema.builder().booleanSchema().build();
        assertEquals(true, PortSchema.mapToSchema(true, boolSchema));
        assertEquals(true, PortSchema.mapToSchema("true", boolSchema));
        assertEquals(true, PortSchema.mapToSchema("yes", boolSchema));
        assertEquals(true, PortSchema.mapToSchema("1", boolSchema));
        assertEquals(false, PortSchema.mapToSchema("false", boolSchema));
        assertEquals(false, PortSchema.mapToSchema(0, boolSchema));
    }

    @Test
    void testMapToSchemaNested() {
        // Address schema
        Map<String, PortSchema> addressProps = new HashMap<>();
        addressProps.put("street", PortSchema.builder().stringSchema().build());
        addressProps.put("zipCode", PortSchema.builder().intSchema().build());
        PortSchema addressSchema = PortSchema.builder().objectSchema(addressProps).build();

        // Scores
        PortSchema intItemSchema = PortSchema.builder().intSchema().build();
        PortSchema scoresSchema = PortSchema.builder().arraySchema(intItemSchema).build();

        // Create person schema
        Map<String, PortSchema> personProps = new HashMap<>();
        personProps.put("name", PortSchema.builder().stringSchema().build());
        personProps.put("addresses", PortSchema.builder().arraySchema(addressSchema).build());
        personProps.put("scores", scoresSchema);
        PortSchema personSchema = PortSchema.builder().objectSchema(personProps).build();


        Map<String, Object> address1 = new HashMap<>();
        address1.put("street", "Main St");
        address1.put("zipCode", "12345");

        Map<String, Object> address2 = new HashMap<>();
        address2.put("street", "Main St");
        address2.put("zipCode", "12345");


        Map<String, Object> person = new HashMap<>();
        person.put("name", "John");
        person.put("addresses", List.of(address1, address2));
        person.put("scores", List.of("1", "2", "3"));

        // Test mapping
        Map<?, ?> result = (Map<?, ?>) PortSchema.mapToSchema(person, personSchema);
        assertEquals("John", result.get("name"));

        List<?> resultAddresses = (List<?>) result.get("addresses");
        assertNotNull(resultAddresses);

        for (var resultAddress : resultAddresses) {
            assertInstanceOf(Map.class, resultAddress);
            assertEquals("Main St", ((Map<?,?>)resultAddress).get("street"));
            assertEquals(12345, ((Map<?,?>)resultAddress).get("zipCode"));
        }

        List<?> resultScores = (List<?>) result.get("scores");
        assertNotNull(resultScores);
        for (var resultScore : resultScores) assertInstanceOf(Integer.class, resultScore);
    }
}