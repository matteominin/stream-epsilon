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
}