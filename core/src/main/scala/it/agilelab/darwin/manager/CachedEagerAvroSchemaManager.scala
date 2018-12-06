package it.agilelab.darwin.manager

import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

/**
  * Implementation of CachedAvroSchemaManager that loads all the schemas into the cache at startup and doesn't
  * perform any other accesses to the storage: each retrieve is performed onto the cache.
  */
class CachedEagerAvroSchemaManager (connector: Connector) extends CachedAvroSchemaManager(connector) {
  override def getSchema(id: Long): Option[Schema] = cache.getSchema(id)
}
