package it.agilelab.darwin.manager

import it.agilelab.darwin.common.Logging
import org.apache.avro.Schema

/**
  * Implementation of [[AvroSchemaCache]] that uses Fingerprint64 as IDs.
  *
  * @param schemas a sequence of (ID, schema) used to initialize the cache values
  */
case class AvroSchemaCacheFingerprint(schemas: Seq[(Long, Schema)], fingerPrinter: Schema => Long)
    extends AvroSchemaCache(schemas)
    with Logging {
  log.debug(s"initialization of the cache with ${schemas.size} schemas")
  private val _table: Map[Long, Schema] = schemas.toMap
  log.debug("cache initialized")

  override def getSchema(id: Long): Option[Schema] = _table.get(id)

  override def contains(schema: Schema): (Boolean, Long) = {
    val id = fingerPrinter(schema)
    _table.contains(id) -> id
  }

  override def insert(values: Seq[(Long, Schema)]): AvroSchemaCache =
    AvroSchemaCacheFingerprint(_table.toSeq ++ values, fingerPrinter)

  /**
    * Retrieves all registered schemas
    *
    * @return A Sequence of (ID, Schema)
    */
  override def getAll: Seq[(Long, Schema)] = _table.toSeq
}
