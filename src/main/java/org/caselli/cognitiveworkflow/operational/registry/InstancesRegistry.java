package org.caselli.cognitiveworkflow.operational.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class InstancesRegistry<T> {
    protected final Map<String, T> runningInstances = new ConcurrentHashMap<>();

    public Optional<T> get(String id) {
        return Optional.ofNullable(runningInstances.get(id));
    }

    public void register(String id, T instance) {
        runningInstances.put(id, instance);
    }

    public void remove(String id) {
        runningInstances.remove(id);
    }


    public void clear() {
        runningInstances.clear();
    }

    public Collection<T> list() {
        return runningInstances.values();
    }
}