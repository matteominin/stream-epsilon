package org.caselli.cognitiveworkflow.operational;
import java.util.HashMap;
import java.util.Map;

public class ExecutionContext extends HashMap<String, Object> {

    /**
     * Retrieves a value from a Map using dot notation for nested keys.
     * Handles cases where intermediate keys are null or not Maps.
     * @param key The dot-separated key (e.g., "data.user.name").
     * @return The value found at the specified path, or null if the path is invalid or the value is null.
     */
    public Object getByDotNotation(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = this;
        Object currentValue = null;

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];
            if (currentMap == null) return null;

            currentValue = currentMap.get(currentKey);

            if (i < keys.length - 1) {
                if (currentValue instanceof Map) {
                    // Cast is safe because we checked instanceof Map
                    //noinspection unchecked
                    currentMap = (Map<String, Object>) currentValue;
                } else {
                    System.out.println("Key '" + currentKey + "' is not a Map, returning null");
                    return null;
                }
            }
        }

        return currentValue;
    }

    /**
     * Sets a value in a Map using dot notation for nested keys.
     * Creates nested Maps if they do not exist.
     * Overwrites existing non-Map values if they are encountered along the path before the last key.
     * @param key The dot-separated key (e.g., "data.user.name").
     * @param value The value to set at the specified path.
     */
    public void putByDotNotation(String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = this;

        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];

            if (i < keys.length - 1) {
                // If not the last key, ensure the current map contains a Map for the next level
                Object nextLevel = currentMap.get(currentKey);
                if (!(nextLevel instanceof Map)) {
                    if(nextLevel != null)
                        System.out.println("Key '" + currentKey + "' is not a Map, overwriting with a new Map");

                    // If the next level is not a Map (or null), create a new Map and put it there
                    nextLevel = new HashMap<String, Object>();
                    currentMap.put(currentKey, nextLevel);
                }

                // Cast is safe because we ensured nextLevel is a Map
                //noinspection unchecked
                currentMap = (Map<String, Object>) nextLevel;
            } else {
                currentMap.put(currentKey, value);
            }
        }
    }
}