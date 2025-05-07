package org.caselli.cognitiveworkflow.operational;

import java.util.HashMap;
import java.util.Map;

/**
 * ExecutionContext extends HashMap to provide enhanced put and get operations
 * that support dot notation for accessing and manipulating nested Map structures.
 */
public class ExecutionContext extends HashMap<String, Object> {

    /**
     * Put a value in the context.
     * The value is placed at the specified nested path within the context.
     * Intermediate Maps are created if they do not exist along the path.
     * If an intermediate key maps to a non-Map value, that value is overwritten
     * with a new HashMap to continue the path.
     * @param key The key, potentially using dot notation (e.g., "data.user.name").
     * @param value The value to associate with the key.
     * @return Always returns null.
     */
    @Override
    public Object put(String key, Object value) {
        if (key == null) return super.put(null, value);
        putByDotNotation(key, value);
        return null;
    }

    /**
     * Overrides the standard get method to support dot notation keys.
     * Retrieves the value located at the specified nested path within the context.
     * @param key The key to retrieve, potentially using dot notation (e.g., "data.user.name").
     * @return The value found at the specified path.
     */
    @Override
    public Object get(Object key) {
        // Apply dot notation logic only if the key is a non-null String
        if (key instanceof String) {
            return getByDotNotation((String) key);
        } else {
            // For null keys or non-String keys, delegate to the super class's get method
            return super.get(key);
        }
    }

    /**
     * Overrides the standard putAll method to support dot notation in the keys of the provided map.
     * Each entry in the map is processed through the put method to ensure dot notation is handled.
     * @param m The map whose entries are to be added to this context.
     */
    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (m == null) return;

        for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }



    /**
     * Overrides the standard containsKey method to support dot notation keys.
     * @param key The key to check, potentially using dot notation (e.g., "data.user.name").
     * @return If a value found at the specified path.
     */
    @Override
    public boolean containsKey(Object key) {
        // Apply dot notation logic only if the key is a non-null String
        if (key instanceof String) {
            var value = getByDotNotation((String) key);
            return value != null;
        } else {
            // For null keys or non-String keys, delegate to the super class's get method
            return super.containsKey(key);
        }
    }

    /**
     * Overrides the standard remove method to support dot notation keys.
     * Removes the mapping for the specified key from this map if present.
     * @param key The key to be removed, potentially using dot notation (e.g., "data.user.name").
     * @return The previous value associated with the key, or null if there was no mapping.
     */
    @Override
    public Object remove(Object key) {
        if (!(key instanceof String keyStr)) return super.remove(key);

        String[] keys = keyStr.split("\\.");

        if (keys.length == 1) return super.remove(keyStr);

        // For nested keys we have to navigate to the parent map
        Map<String, Object> parentMap = navigateToParentMap(keys);
        if (parentMap == null) return null;

        // Remove the key from the parent map
        String lastKey = keys[keys.length - 1];
        return parentMap.remove(lastKey);
    }

    /**
     * Helper: navigates to the parent map for the given key path.
     * @param keys Array of keys representing the path
     * @return The parent map containing the last key, or null if path doesn't exist
     */
    private Map<String, Object> navigateToParentMap(String[] keys) {
        Map<String, Object> currentMap = this;

        for (int i = 0; i < keys.length - 1; i++) {
            Object nextLevel;

            if (currentMap == this) {
                nextLevel = super.get(keys[i]);
            } else {
                nextLevel = currentMap.get(keys[i]);
            }

            if (!(nextLevel instanceof Map)) return null;

            //noinspection unchecked
            currentMap = (Map<String, Object>) nextLevel;
        }

        return currentMap;
    }

    /**
     * Overrides the standard getOrDefault method to support dot notation keys.
     * @param key The key to retrieve, potentially using dot notation (e.g., "data.user.name").
     * @param defaultValue The default value to return if the key is not found.
     * @return The value found at the specified path, or the defaultValue if no mapping exists.
     */
    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (!(key instanceof String)) return super.getOrDefault(key, defaultValue);

        Object value = getByDotNotation((String) key);
        return value != null ? value : defaultValue;
    }

    /**
     * Prints the entire context structure in a readable format with indentation
     * to show nesting levels.
     */
    public void printContext() {
        printContext(this, 0);
    }

    private Object getByDotNotation(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = this; // Start traversal from the current instance

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];
            if (currentMap == null)
                return null; // Should theoretically not be reached if starting with 'this'


            Object currentValue;
            // Use super.get() when currentMap is 'this' to avoid infinite recursion
            if (currentMap == this) {
                currentValue = super.get(currentKey);
            } else {
                // For nested maps we can use regular get()
                currentValue = currentMap.get(currentKey);
            }

            if (i < keys.length - 1) {
                // If it's an intermediate key, the value must be a Map to continue the path
                if (currentValue instanceof Map) {
                    //noinspection unchecked
                    currentMap = (Map<String, Object>) currentValue;
                } else {
                    // Path is broken because an intermediate key leads to a non-Map value
                    return null;
                }
            } else {
                return currentValue;
            }
        }

        return null;
    }

    private void putByDotNotation(String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = this; // Start traversal from the current instance

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];

            if (i < keys.length - 1) {
                Object nextLevel;

                // Use super.get() when currentMap is 'this' to break recursion
                if (currentMap == this) {
                    nextLevel = super.get(currentKey);
                } else {
                    // For nested maps we can use regular get()
                    nextLevel = currentMap.get(currentKey);
                }

                if (!(nextLevel instanceof Map)) {
                    nextLevel = new HashMap<String, Object>();
                    if (currentMap == this) {
                        super.put(currentKey, nextLevel);
                    } else {
                        currentMap.put(currentKey, nextLevel);
                    }
                }

                //noinspection unchecked
                currentMap = (Map<String, Object>) nextLevel;

            } else {
                if (currentMap == this) {
                    super.put(currentKey, value);
                } else {
                    currentMap.put(currentKey, value);
                }
            }
        }
    }

    private void printContext(Map<String, Object> map, int indentLevel) {
        if (map == null || map.isEmpty()) {
            indent(indentLevel);
            System.out.println("{}");
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            indent(indentLevel);
            System.out.print(entry.getKey() + ": ");

            if (entry.getValue() instanceof Map) {
                System.out.println("{");
                //noinspection unchecked
                printContext((Map<String, Object>) entry.getValue(), indentLevel + 1);
                indent(indentLevel);
                System.out.println("}");
            } else {
                System.out.println(toString(entry.getValue()));
            }
        }
    }

    private void indent(int level) {
        for (int i = 0; i < level; i++)
            System.out.print("    ");
    }

    private String toString(Object obj) {
        if (obj == null) return "null";
        return obj.toString();
    }
}