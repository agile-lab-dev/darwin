package it.agilelab.darwin.manager

import org.apache.avro.Schema

/**
  * Generic definition of the cache used by the manager to store the data loaded from the external storage.
  * @param schemas a sequence of (ID, schema) used to initialize the cache values
  */
abstract class AvroSchemaCache(schemas: Seq[(Long, Schema)]) {

  /**
    * Retrieves a registered schema for the input ID.
    *
    * @param id the Long ID of the schema
    * @return the Schema associated to the input ID
    */
  def getSchema(id: Long): Option[Schema]

  /**
    * Tests if the input schema is contained inside the cache.
    *
    * @param schema a Schema that the cache could contain
    * @return a pair containing: a boolean that is true if the schema is contained in the cache and the ID of the
    *         schema in any case
    */
  def contains(schema: Schema): (Boolean, Long)

  /**
    * Creates a new instance of [[AvroSchemaCache]] with the original values plus the input ones.
    *
    * @param values new pair (ID, schema) to insert inside the cache
    * @return a new instance of [[AvroSchemaCache]] containing the new values in addition to the original ones.
    */
  def insert(values: Seq[(Long, Schema)]): AvroSchemaCache

  /**
    * Retrieves all registered schemas
    *
    * @return A Sequence of (ID, Schema)
    */
  def getAll: Seq[(Long, Schema)]
}
