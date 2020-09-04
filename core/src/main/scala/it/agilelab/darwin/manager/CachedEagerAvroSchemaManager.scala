package it.agilelab.darwin.manager

import java.nio.ByteOrder

import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

/**
 * Implementation of CachedAvroSchemaManager that loads all the schemas into the cache at startup and doesn't
 * perform any other accesses to the storage: each retrieve is performed onto the cache.
 */
class CachedEagerAvroSchemaManager(connector: Connector, endianness: ByteOrder)
  extends CachedAvroSchemaManager(connector, endianness) {
  def this(connector: Connector) = this(connector, ByteOrder.BIG_ENDIAN)

  override def getSchema(id: Long): Option[Schema] = cache.getSchema(id)
}
