package org.caselli.cognitiveworkflow.operational;

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
    void testGetByDotNotation_simpleKey() {
        context.put("name", "Test User");
        assertEquals("Test User", context.getByDotNotation("name"));
    }

    @Test
    void testGetByDotNotation_nestedKey() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test User");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        assertEquals("Test User", context.getByDotNotation("data.user.name"));
    }

    @Test
    void testGetByDotNotation_intermediateKeyNotFound() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", "Test User");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        // Path: data -> nonExistent -> name
        assertNull(context.getByDotNotation("data.nonExistent.name"));
    }

    @Test
    void testGetByDotNotation_rootKeyNotFound() {
        // Path: nonExistent -> user -> name
        assertNull(context.getByDotNotation("nonExistent.user.name"));
    }

    @Test
    void testGetByDotNotation_intermediateKeyNotMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", "not a map"); // "user" is a String, not a Map
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.getByDotNotation("data.user.name"));
    }

    @Test
    void testGetByDotNotation_intermediateKeyIsNull() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", null); // "user" is null
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.getByDotNotation("data.user.name"));
    }


    @Test
    void testGetByDotNotation_lastKeyIsNull() {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", null); // "name" is null
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        // Path: data -> user -> name
        assertNull(context.getByDotNotation("data.user.name"));
    }

    @Test
    void testGetByDotNotation_emptyContext() {
        assertNull(context.getByDotNotation("any.key"));
        assertNull(context.getByDotNotation("simpleKey"));
    }

    @Test
    void testGetByDotNotation_emptyKey() {
        context.put("", "emptyKeyVal");
        assertEquals("emptyKeyVal", context.getByDotNotation(""));
        assertNull(context.getByDotNotation("."));
        assertNull(context.getByDotNotation(".."));
    }

    @Test
    void testGetByDotNotation_keyWithOnlyDots() {
        assertNull(context.getByDotNotation("."));
        assertNull(context.getByDotNotation(".."));
    }


    // --- Tests for putByDotNotation ---

    @Test
    void testPutByDotNotation_simpleKey() {
        context.putByDotNotation("name", "New User");
        assertEquals("New User", context.get("name"));
    }

    @Test
    void testPutByDotNotation_nestedKey_pathExists() {
        Map<String, Object> userMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        context.putByDotNotation("data.user.name", "New User Nested");

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertEquals("New User Nested", ((Map<?, ?>) user).get("name"));
    }

    @Test
    void testPutByDotNotation_nestedKey_pathCreatesMaps() {
        context.putByDotNotation("new.nested.path.value", "Deep Value");

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
    void testPutByDotNotation_intermediateKeyNotMap_overwrites() {
        context.put("data", "not a map"); // Initial non-Map value

        context.putByDotNotation("data.user.name", "Value After Overwrite");

        Object data = context.get("data");
        assertInstanceOf(Map.class, data); // Should now be a Map
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertEquals("Value After Overwrite", ((Map<?, ?>) user).get("name"));
    }

    @Test
    void testPutByDotNotation_rootKeyNotMap_overwrites() {
        context.put("targetRoot", "initial_string"); // Initial non-Map value at the root

        context.putByDotNotation("targetRoot.nestedKey", "Value After Overwrite");

        Object targetRoot = context.get("targetRoot");
        assertInstanceOf(Map.class, targetRoot); // Should now be a Map
        Object nestedValue = ((Map<?, ?>) targetRoot).get("nestedKey");
        assertEquals("Value After Overwrite", nestedValue);
    }


    @Test
    void testPutByDotNotation_putNullValue() {
        Map<String, Object> userMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("user", userMap);
        context.put("data", dataMap);

        context.putByDotNotation("data.user.name", null);

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object user = ((Map<?, ?>) data).get("user");
        assertInstanceOf(Map.class, user);
        assertNull(((Map<?, ?>) user).get("name"));
    }

    @Test
    void testPutByDotNotation_emptyContext() {
        context.putByDotNotation("first.value", "Initial");
        Object first = context.get("first");
        assertInstanceOf(Map.class, first);
        assertEquals("Initial", ((Map<?, ?>) first).get("value"));
    }

    @Test
    void testPutByDotNotation_emptyKey() {
        context.putByDotNotation("", "EmptyKeyVal");
        assertEquals("EmptyKeyVal", context.get(""));
    }

    @Test
    void testPutByDotNotation_putComplexObject() {
        Map<String, Object> complexValue = Map.of("list", List.of(1, 2, 3), "boolean", true);
        context.putByDotNotation("data.complex", complexValue);

        Object data = context.get("data");
        assertInstanceOf(Map.class, data);
        Object complex = ((Map<?, ?>) data).get("complex");
        assertInstanceOf(Map.class, complex);
        assertEquals(complexValue, complex);
    }
}