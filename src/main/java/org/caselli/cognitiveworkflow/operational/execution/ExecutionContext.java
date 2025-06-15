package org.caselli.cognitiveworkflow.operational.execution;

import lombok.NoArgsConstructor;

import java.util.*;

/**
 * ExecutionContext extends HashMap to provide enhanced put and get operations
 * that support dot notation for accessing and manipulating nested Map structures
 * and List/array elements using numeric indices.
 */
@NoArgsConstructor
public class ExecutionContext extends HashMap<String, Object> {


    /**
     * Put a value in the context.
     * The value is placed at the specified nested path within the context.
     * Intermediate Maps and Lists are created if they do not exist along the path.
     * If an intermediate key maps to a non-Map/non-List value, that value is overwritten
     * with a new HashMap or ArrayList to continue the path.
     * @param key The key, potentially using dot notation (e.g., "data.users.0.name").
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
     * Overrides the standard get method to support dot notation keys with array indices.
     * Retrieves the value located at the specified nested path within the context.
     * @param key The key to retrieve, potentially using dot notation (e.g., "data.users.0.name").
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
     * Overrides the standard containsKey method to support dot notation keys with array indices.
     * @param key The key to check, potentially using dot notation (e.g., "data.users.0.name").
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
     * Overrides the standard remove method to support dot notation keys with array indices.
     * Removes the mapping for the specified key from this map if present.
     * @param key The key to be removed, potentially using dot notation (e.g., "data.users.0.name").
     * @return The previous value associated with the key, or null if there was no mapping.
     */
    @Override
    public Object remove(Object key) {
        if (!(key instanceof String keyStr)) return super.remove(key);

        String[] keys = keyStr.split("\\.");

        if (keys.length == 1) return super.remove(keyStr);

        // For nested keys we have to navigate to the parent container
        NavigationResult parentResult = navigateToParentContainer(keys);
        if (parentResult == null) return null;

        // Remove the key from the parent container
        String lastKey = keys[keys.length - 1];
        return removeFromContainer(parentResult.container, lastKey);
    }

    /**
     * Overrides the standard getOrDefault method to support dot notation keys with array indices.
     * @param key The key to retrieve, potentially using dot notation (e.g., "data.users.0.name").
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
     * Copy constructor that creates a deep copy of another ExecutionContext.
     * This ensures that nested Maps and Lists are also copied, preventing
     * shared references between the original and copied contexts.
     * @param other The ExecutionContext to copy from
     */
    public ExecutionContext(ExecutionContext other) {
        super();
        if (other != null) deepCopyFrom(other);
    }

    /**
     * Helper method to perform deep copy of the context structure.
     * Recursively copies all nested Maps and Lists to ensure complete isolation.
     *
     * @param source The source context to copy from
     */
    private void deepCopyFrom(ExecutionContext source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            super.put(key, deepCopyValue(value));
        }
    }

    /**
     * Recursively creates deep copies of nested structures (Maps and Lists).
     * @param value The value to copy
     * @return A deep copy of the value
     */
    private Object deepCopyValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sourceMap = (Map<String, Object>) value;
            Map<String, Object> copiedMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                copiedMap.put(entry.getKey(), deepCopyValue(entry.getValue()));
            }

            return copiedMap;
        }

        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> sourceList = (List<Object>) value;
            List<Object> copiedList = new ArrayList<>();

            for (Object item : sourceList) {
                copiedList.add(deepCopyValue(item));
            }

            return copiedList;
        }
        return value;
    }






    /**
     * Prints the entire context structure in a readable format with indentation
     * to show nesting levels.
     */
    public void printContext() {
        printContext(this, 0);
    }

    /**
     * Helper class to hold navigation results
     */
    private static class NavigationResult {
        Object container;
        boolean isMap;

        NavigationResult(Object container, boolean isMap) {
            this.container = container;
            this.isMap = isMap;
        }
    }

    /**
     * Helper: navigates to the parent container for the given key path.
     * Example: with a path like "user.profiles.0.name" the method navigates to the list at "profiles.0".
     * @param keys Array of keys representing the path. Example [user, profiles, 0, name]
     * @return NavigationResult containing the parent container, or null if path doesn't exist
     */
    private NavigationResult navigateToParentContainer(String[] keys) {
        Object currentContainer = this;
        boolean isMap = true;

        for (int i = 0; i < keys.length - 1; i++) {
            String currentKey = keys[i];
            Object nextLevel = getFromContainer(currentContainer, currentKey, isMap);

            if (nextLevel == null) return null;

            if (nextLevel instanceof Map) {
                currentContainer = nextLevel;
                isMap = true;
            } else if (nextLevel instanceof List) {
                currentContainer = nextLevel;
                isMap = false;
            } else {
                return null;
            }
        }

        return new NavigationResult(currentContainer, isMap);
    }

    /**
     * Helper method to get a value from the context using a key which may use dot notation with array indices.
     * @param key The key in dot notation
     * @return The retrieved value
     */
    private Object getByDotNotation(String key) {
        String[] keys = key.split("\\.");
        Object currentContainer = this;
        boolean isMap = true;

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];
            if (currentContainer == null) return null;

            Object currentValue = getFromContainer(currentContainer, currentKey, isMap);

            if (i < keys.length - 1) {
                // If it's an intermediate key, the value must be a Map or List to continue the path
                if (currentValue instanceof Map) {
                    currentContainer = currentValue;
                    isMap = true;
                } else if (currentValue instanceof List) {
                    currentContainer = currentValue;
                    isMap = false;
                } else {
                    // Path is broken because an intermediate key doesn't lead to a container
                    return null;
                }
            } else {
                return currentValue;
            }
        }

        return null;
    }

    /**
     * Helper method to put a value in the context using a key which may use dot notation with array indices.
     * @param key The key in dot notation
     * @param value The value to insert
     */
    private void putByDotNotation(String key, Object value) {
        String[] keys = key.split("\\.");
        Object currentContainer = this;
        boolean isMap = true;

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];

            if (i < keys.length - 1) {
                Object nextLevel = getFromContainer(currentContainer, currentKey, isMap);

                if ((!isContainer(nextLevel))) {
                    // Determine what type of container to create based on the next key
                    String nextKey = keys[i + 1];
                    boolean createList = isNumeric(nextKey);

                    nextLevel = createList ? new ArrayList<>() : new HashMap<String, Object>();
                    putInContainer(currentContainer, currentKey, nextLevel, isMap);
                }

                // Update current container reference
                currentContainer = nextLevel;
                isMap = nextLevel instanceof Map;
            } else {
                // Final key - put the value
                putInContainer(currentContainer, currentKey, value, isMap);
            }
        }
    }

    /**
     * Helper method to get a value from either a Map or List container
     */
    private Object getFromContainer(Object container, String key, boolean isMap) {
        if (isMap) {
            if (container == this) {
                return super.get(key); // Avoid recursion
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) container;
                return map.get(key);
            }
        } else {
            // List access
            if (!isNumeric(key)) return null;

            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) container;
            int index = Integer.parseInt(key);

            if (index < 0 || index >= list.size()) return null;
            return list.get(index);
        }
    }

    /**
     * Helper method to put a value into either a Map or List container
     */
    private void putInContainer(Object container, String key, Object value, boolean isMap) {
        if (isMap) {
            if (container == this) {
                super.put(key, value); // Avoid recursion
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) container;
                map.put(key, value);
            }
        } else {
            // List access
            if (!isNumeric(key)) return;

            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) container;
            int index = Integer.parseInt(key);

            // Expand list if necessary
            while (list.size() <= index) {
                list.add(null);
            }

            list.set(index, value);
        }
    }

    /**
     * Helper method to remove a value from either a Map or List container
     */
    private Object removeFromContainer(Object container, String key) {
        if (container instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) container;
            return map.remove(key);
        } else if (container instanceof List) {
            if (!isNumeric(key)) return null;

            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) container;
            int index = Integer.parseInt(key);

            if (index < 0 || index >= list.size()) return null;
            return list.remove(index);
        }
        return null;
    }

    /**
     * Check if a string represents a valid numeric index
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if an object is a container (Map or List)
     */
    private boolean isContainer(Object obj) {
        return obj instanceof Map || obj instanceof List;
    }

    /**
     * An helper method to print a container with indentation (for debug purposes)
     * @param container The container to print (Map or List)
     * @param indentLevel The level of indentation
     */
    private void printContext(Object container, int indentLevel) {
        if (container == null) {
            indent(indentLevel);
            System.out.println("null");
            return;
        }

        if (container instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) container;

            if (map.isEmpty()) {
                indent(indentLevel);
                System.out.println("{}");
                return;
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                indent(indentLevel);
                System.out.print(entry.getKey() + ": ");

                if (isContainer(entry.getValue())) {
                    System.out.println(entry.getValue() instanceof Map ? "{" : "[");
                    printContext(entry.getValue(), indentLevel + 1);
                    indent(indentLevel);
                    System.out.println(entry.getValue() instanceof Map ? "}" : "]");
                } else {
                    System.out.println(toString(entry.getValue()));
                }
            }
        } else if (container instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) container;

            if (list.isEmpty()) {
                indent(indentLevel);
                System.out.println("[]");
                return;
            }

            for (int i = 0; i < list.size(); i++) {
                indent(indentLevel);
                System.out.print("[" + i + "]: ");

                Object value = list.get(i);
                if (isContainer(value)) {
                    System.out.println(value instanceof Map ? "{" : "[");
                    printContext(value, indentLevel + 1);
                    indent(indentLevel);
                    System.out.println(value instanceof Map ? "}" : "]");
                } else {
                    System.out.println(toString(value));
                }
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