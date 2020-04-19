package it.agilelab.darwin.manager

import it.agilelab.darwin.common.Logging
import org.apache.avro.{Schema, SchemaNormalization}

/**
  * Implementation of [[AvroSchemaCache]] that uses Fingerprint64 as IDs.
  *
  * @param schemas a sequence of (ID, schema) used to initialize the cache values
  */
case class AvroSchemaCacheFingerprint(schemas: Seq[(Long, SchemaAndVersion)])
  extends AvroSchemaCache(schemas) with Logging {

  log.debug(s"initialization of the cache with ${schemas.size} schemas")
  private val _table: Map[Long, SchemaAndVersion] = schemas.toMap
  log.debug("cache initialized")

  override def getSchema(id: Long): Option[Schema] = _table.get(id).map(_.schema)

  override def contains(schema: Schema): (Boolean, Long) = {
    val id = SchemaNormalization.parsingFingerprint64(schema)
    _table.get(id).isDefined -> id
  }

  override def insert(values: Seq[(Long, SchemaAndVersion)]): AvroSchemaCache =
    AvroSchemaCacheFingerprint(_table.toSeq ++ values)

  /**
    * Retrieves all registered schemas
    *
    * @return A Sequence of (ID, Schema)
    */
  override def getAll: Seq[(Long, Schema)] = _table.toSeq.map {
    case (l, version) => l -> version.schema
  }

  /**
    * Retrieves all registered schemas with their version
    *
    * @return A Sequence of (ID, Schema)
    */
  override def getAllWithVersion: Seq[(Long, SchemaAndVersion)] = _table.toSeq
}
