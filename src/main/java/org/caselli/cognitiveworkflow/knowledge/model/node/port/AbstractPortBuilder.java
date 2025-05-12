package org.caselli.cognitiveworkflow.knowledge.model.node.port;

public abstract class AbstractPortBuilder<T extends Port, B extends AbstractPortBuilder<T, B>> {
    protected String key;
    protected PortSchema schema;
    protected Object defaultValue;

    public B withKey(String key) {
        this.key = key;
        return self();
    }

    public B withSchema(PortSchema schema) {
        this.schema = schema;
        return self();
    }

    public B withDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return self();
    }

    protected abstract B self();
    protected abstract T createInstance();

    public T build() {
        if (key == null || key.isEmpty()) throw new IllegalStateException("Key must be specified");
        if (schema == null) throw new IllegalStateException("Schema must be specified");
        if (defaultValue != null && !schema.isValidValue(defaultValue)) throw new IllegalStateException("Default value is not valid for the schema");

        T port = createInstance();
        port.setKey(key);
        port.setSchema(schema);
        port.setDefaultValue(defaultValue);
        return port;
    }
}
