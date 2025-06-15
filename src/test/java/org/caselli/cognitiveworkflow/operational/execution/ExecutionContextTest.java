package org.caselli.cognitiveworkflow.operational.execution;

import org.caselli.cognitiveworkflow.operational.execution.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("test")
class ExecutionContextTest {

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        context = new ExecutionContext();
    }


    @Test
    void testSimpleKey() {
        context.put("name", "Test User");
        assertEquals("Test User", context.get("name"));
    }

    @Test
    void testNestedKey() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test User");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        assertEquals("Test User", context.get("data.user.name"));
    }

    @Test
    void testIntermediateKeyNotFound() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test User");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        // Path: data -> nonExistent -> name
        assertNull(context.get("data.nonExistent.name"));
    }

    @Test
    void testRootKeyNotFound() {
        // Path: nonExistent -> user -> name
        assertNull(context.get("nonExistent.user.name"));
    }

    @Test
    void testIntermediateKeyNotMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", "not a map"); // "user" is a String, not a Map
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.get("data.user.name"));
    }

    @Test
    void testIntermediateKeyIsNull() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", null); // "user" is null
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.get("data.user.name"));
    }


    @Test
    void testLastKeyIsNull() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", null); // "name" is null
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.get("data.user.name"));
    }

    @Test
    void testGetEmptyKey() {
        context.put("", "emptyKeyVal");
        assertEquals("emptyKeyVal", context.get(""));
        assertNull(context.get("."));
        assertNull(context.get(".."));
    }

    @Test
    void testKeyWithOnlyDots() {
        assertNull(context.get("."));
        assertNull(context.get(".."));
    }

    @Test
    void testNestedKey_pathExists() {
        Map<String, Object> userMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        context.put("data.user.name", "New User Nested");

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertEquals("New User Nested", ((Map<?, ?>) user).get("name"));
    }

    @Test
    void testNestedKey_pathCreatesMaps() {
        context.put("new.nested.path.value", "Deep Value");

        Object level1 = context.get("new");
        assertInstanceOf(Map.class, level1);
        Object level2 = ((Map<?, ?>) level1).get("nested");
        assertInstanceOf(Map.class, level2);
        Object level3 = ((Map<?, ?>) level2).get("path");
        assertInstanceOf(Map.class, level3);
        Object finalValue = ((Map<?, ?>) level3).get("value");
        assertEquals("Deep Value", finalValue);
    }

    @Test
    void testIntermediateKeyNotMap_overwrites() {
        context.put("data", "not a map"); // Initial non-Map value

        context.put("data.user.name", "Value After Overwrite");

        Object data = context.get("data");
        assertInstanceOf(Map.class, data); // Should now be a Map
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertEquals("Value After Overwrite", ((Map<?, ?>) user).get("name"));
    }

    @Test
    void testRootKeyNotMap_overwrites() {
        context.put("targetRoot", "initial_string"); // Initial non-Map value at the root

        context.put("targetRoot.nestedKey", "Value After Overwrite");

        Object targetRoot = context.get("targetRoot");
        assertInstanceOf(Map.class, targetRoot); // Should now be a Map
        Object nestedValue = ((Map<?, ?>) targetRoot).get("nestedKey");
        assertEquals("Value After Overwrite", nestedValue);
    }


    @Test
    void testPutNullValue() {
        Map<String, Object> userMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        context.put("data.user.name", null);

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertNull(((Map<?, ?>) user).get("name"));
    }

    @Test
    void testEmptyContext() {
        context.put("first.value", "Initial");
        Object first = context.get("first");
        assertInstanceOf(Map.class, first);
        assertEquals("Initial", ((Map<?, ?>) first).get("value"));
    }

    @Test
    void testEmptyKey() {
        context.put("", "EmptyKeyVal");
        assertEquals("EmptyKeyVal", context.get(""));
    }

    @Test
    void testPutComplexObject() {
        Map<String, Object> complexValue = Map.of("list", List.of(1, 2, 3), "boolean", true);
        context.put("data.complex", complexValue);

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object complex = ((Map<?, ?>) data).get("complex");
        assertInstanceOf(Map.class, complex);
        assertEquals(complexValue, complex);

        context.printContext();
    }

    @Test
    void testPutComplexNestedObject() {
        context.put("user.userDetails.email", "email");
        context.put("user.userDetails.phone", "+39");
        context.put("user.id", "Niccolò");
        context.put("order.id", "123");

        assertEquals(context.get("user.userDetails.email"), "email");
        assertEquals(context.get("user.userDetails.phone"), "+39");
        assertEquals(context.get("user.id"), "Niccolò");
        assertEquals(context.get("order.id"), "123");

        context.printContext();
    }

    @Test
    void testPutAll() {
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("user.profile.name", "Marco Rossi");
        sourceMap.put("user.profile.email", "marco@example.com");
        sourceMap.put("settings.notifications", true);
        sourceMap.put("simple", "value");
        context.putAll(sourceMap);

        // Verify
        assertEquals("Marco Rossi", context.get("user.profile.name"));
        assertEquals("marco@example.com", context.get("user.profile.email"));
        assertEquals(true, context.get("settings.notifications"));
        assertEquals("value", context.get("simple"));

        Object settings = context.get("settings");
        assertInstanceOf(Map.class, settings);
        assertEquals(true, ((Map<?, ?>) settings).get("notifications"));
    }

    @Test
    void testRemove() {
        context.put("user.profile.name", "Marco Rossi");
        context.put("user.profile.email", "marco@example.com");
        context.put("user.id", "12345");

        // Test removing leaf node
        Object removedEmail = context.remove("user.profile.email");
        assertEquals("marco@example.com", removedEmail);
        assertNull(context.get("user.profile.email"));

        // Verify other paths still exist
        assertEquals("Marco Rossi", context.get("user.profile.name"));
        assertEquals("12345", context.get("user.id"));

        // Test removing non-existent path
        assertNull(context.remove("user.profile.nonexistent"));

        // Test removing intermediate node
        Object removedProfile = context.remove("user.profile");
        assertInstanceOf(Map.class, removedProfile);
        assertNull(context.get("user.profile"));
        assertNull(context.get("user.profile.name"));

        // Verify root path still exists
        assertInstanceOf(Map.class, context.get("user"));
        assertEquals("12345", context.get("user.id"));

        // Test removing root node
        Object removedUser = context.remove("user");
        assertInstanceOf(Map.class, removedUser);
        assertNull(context.get("user"));
        assertNull(context.get("user.id"));
    }

    @Test
    void testGetOrDefault() {
        context.put("user.profile.name", "Marco Rossi");
        context.put("user.profile.email", "marco@example.com");
        context.put("user.settings.notifications", false);

        // Test existing paths
        assertEquals("Marco Rossi", context.getOrDefault("user.profile.name", "Default Name"));
        assertEquals("marco@example.com", context.getOrDefault("user.profile.email", "default@example.com"));
        assertEquals(false, context.getOrDefault("user.settings.notifications", true));

        // Test non-existent paths
        assertEquals("Default Address", context.getOrDefault("user.profile.address", "Default Address"));
        assertEquals(25, context.getOrDefault("user.age", 25));
        assertEquals("Unknown", context.getOrDefault("nonexistent.path", "Unknown"));

        // Test with null values and defaults
        context.put("user.profile.phone", null);
        assertNotNull(context.getOrDefault("user.profile.phone", "Default Phone"));

        // Test with edge cases
        assertEquals("Root Default", context.getOrDefault("", "Root Default"));
        assertEquals("Dot Default", context.getOrDefault(".", "Dot Default"));
    }

    @Test
    void testPutAndReadDotNotationArrayOfObjects(){
        context.put("user.details.0.name", "Alice");
        context.put("user.details.0.surname", "Smith");
        context.put("user.details.1.name", "Bob");
        context.put("user.details.1.surname", "Johnson");

        System.out.println("Context: " + context);

        assertEquals("Alice", context.get("user.details.0.name"));
        assertEquals("Bob", context.get("user.details.1.name"));
        assertEquals("Smith", context.get("user.details.0.surname"));
        assertEquals("Johnson", context.get("user.details.1.surname"));

        // Verify that the array structure is maintained
        Object userDetails = context.get("user.details");
        assertInstanceOf(List.class, userDetails);
        List<?> detailsList = (List<?>) userDetails;
        assertEquals(2, detailsList.size());
        assertEquals("Alice", ((Map<?, ?>) detailsList.get(0)).get("name"));
        assertEquals("Bob", ((Map<?, ?>) detailsList.get(1)).get("name"));
        assertEquals("Smith", ((Map<?, ?>) detailsList.get(0)).get("surname"));
        assertEquals("Johnson", ((Map<?, ?>) detailsList.get(1)).get("surname"));
    }


    @Test
    void testPutAndReadDotNotationArrayOfStrings(){
        context.put("user.details.0", "Alice");
        context.put("user.details.1", "Bob");

        System.out.println("Context: " + context);

        assertEquals("Alice", context.get("user.details.0"));
        assertEquals("Bob", context.get("user.details.1"));

        // Verify that the array structure is maintained
        Object userDetails = context.get("user.details");
        assertInstanceOf(List.class, userDetails);
        List<?> detailsList = (List<?>) userDetails;
        assertEquals(2, detailsList.size());
        assertEquals("Alice", detailsList.get(0));
        assertEquals("Bob", detailsList.get(1));
    }

    @Test
    void testCopyConstructorComplexStructure() {
        ExecutionContext original = new ExecutionContext();

        original.put("simpleString", "test");
        original.put("simpleNumber", 42);
        original.put("simpleBoolean", true);
        original.put("nullValue", null);
        original.put("user.profile.name", "John Doe");
        original.put("user.profile.email", "john@example.com");
        original.put("user.profile.age", 30);
        original.put("user.settings.notifications", true);
        original.put("user.settings.theme", "dark");
        original.put("user.addresses.0.type", "home");
        original.put("user.addresses.0.street", "123 Main St");
        original.put("user.addresses.0.city", "New York");
        original.put("user.addresses.1.type", "work");
        original.put("user.addresses.1.street", "456 Business Ave");
        original.put("user.addresses.1.city", "Boston");
        original.put("user.hobbies.0", "reading");
        original.put("user.hobbies.1", "swimming");
        original.put("user.hobbies.2", "coding");
        original.put("orders.0.id", "ORD-001");
        original.put("orders.0.items.0.name", "Laptop");
        original.put("orders.0.items.0.price", 999.99);
        original.put("orders.0.items.1.name", "Mouse");
        original.put("orders.0.items.1.price", 25.50);
        original.put("orders.1.id", "ORD-002");
        original.put("orders.1.items.0.name", "Keyboard");
        original.put("orders.1.items.0.price", 75.00);

        // Test copy constructor
        ExecutionContext copy = new ExecutionContext(original);

        assertEquals("test", copy.get("simpleString"));
        assertEquals(42, copy.get("simpleNumber"));
        assertEquals(true, copy.get("simpleBoolean"));
        assertNull(copy.get("nullValue"));
        assertEquals("John Doe", copy.get("user.profile.name"));
        assertEquals("john@example.com", copy.get("user.profile.email"));
        assertEquals(30, copy.get("user.profile.age"));
        assertEquals(true, copy.get("user.settings.notifications"));
        assertEquals("dark", copy.get("user.settings.theme"));
        assertEquals("home", copy.get("user.addresses.0.type"));
        assertEquals("123 Main St", copy.get("user.addresses.0.street"));
        assertEquals("New York", copy.get("user.addresses.0.city"));
        assertEquals("work", copy.get("user.addresses.1.type"));
        assertEquals("456 Business Ave", copy.get("user.addresses.1.street"));
        assertEquals("Boston", copy.get("user.addresses.1.city"));
        assertEquals("reading", copy.get("user.hobbies.0"));
        assertEquals("swimming", copy.get("user.hobbies.1"));
        assertEquals("coding", copy.get("user.hobbies.2"));
        assertEquals("ORD-001", copy.get("orders.0.id"));
        assertEquals("Laptop", copy.get("orders.0.items.0.name"));
        assertEquals(999.99, copy.get("orders.0.items.0.price"));
        assertEquals("Mouse", copy.get("orders.0.items.1.name"));
        assertEquals(25.50, copy.get("orders.0.items.1.price"));
        assertEquals("ORD-002", copy.get("orders.1.id"));
        assertEquals("Keyboard", copy.get("orders.1.items.0.name"));
        assertEquals(75.00, copy.get("orders.1.items.0.price"));

        // Verify deep copy - modifications to copy should not affect original
        copy.put("user.profile.name", "Jane Doe");
        copy.put("user.addresses.0.city", "Los Angeles");
        copy.put("user.hobbies.0", "painting");
        copy.put("orders.0.items.0.price", 1200.00);

        // Original should remain unchanged
        assertEquals("John Doe", original.get("user.profile.name"));
        assertEquals("New York", original.get("user.addresses.0.city"));
        assertEquals("reading", original.get("user.hobbies.0"));
        assertEquals(999.99, original.get("orders.0.items.0.price"));

        // Copy should have new values
        assertEquals("Jane Doe", copy.get("user.profile.name"));
        assertEquals("Los Angeles", copy.get("user.addresses.0.city"));
        assertEquals("painting", copy.get("user.hobbies.0"));
        assertEquals(1200.00, copy.get("orders.0.items.0.price"));

        // Verify structural independence
        copy.put("newSection.data.0.value", "new data");
        original.put("originalSection.info.message", "original only");

        assertNull(original.get("newSection.data.0.value"));
        assertNull(copy.get("originalSection.info.message"));
        assertEquals("new data", copy.get("newSection.data.0.value"));
        assertEquals("original only", original.get("originalSection.info.message"));

        // Verify that removing from copy doesn't affect original
        copy.remove("user.settings");
        assertNotNull(original.get("user.settings.notifications"));
        assertNull(copy.get("user.settings.notifications"));

        // Test null constructor parameter
        ExecutionContext nullCopy = new ExecutionContext(null);
        assertTrue(nullCopy.isEmpty());
    }
}