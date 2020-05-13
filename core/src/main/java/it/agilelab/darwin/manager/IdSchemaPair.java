package it.agilelab.darwin.manager;

import org.apache.avro.Schema;

public class IdSchemaPair {
    private final long id;
    private final Schema schema;

    private IdSchemaPair(long id, Schema schema) {
        this.id = id;
        this.schema = schema;
    }

    public long getId() {
        return id;
    }

    public Schema getSchema() {
        return schema;
    }

    public static IdSchemaPair create(long id, Schema schema) {
        return new IdSchemaPair(id, schema);
    }
}
