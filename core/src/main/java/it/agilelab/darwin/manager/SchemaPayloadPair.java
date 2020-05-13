package it.agilelab.darwin.manager;

import org.apache.avro.Schema;

public class SchemaPayloadPair {
    private final Schema schema;
    private final byte[] payload;

    private SchemaPayloadPair(Schema schema, byte[] payload) {
        this.schema = schema;
        this.payload = payload;
    }

    public Schema getSchema() {
        return schema;
    }

    public byte[] getPayload() {
        return payload;
    }

    public static SchemaPayloadPair create(Schema schema, byte[] payload) {
        return new SchemaPayloadPair(schema, payload);
    }
}
