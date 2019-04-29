package it.agilelab.darwin.manager

import java.nio.ByteOrder

import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

/**
  * Implementation of CachedAvroSchemaManager that loads all the schemas into the cache at startup and perform
  * all the retrieves onto the cache; an access to the storage is performed only if there is a cache miss.
  */
class CachedLazyAvroSchemaManager(connector: Connector, endianness: ByteOrder)
  extends CachedAvroSchemaManager(connector, endianness) {

  override def getSchema(id: Long): Option[Schema] = {
    cache.getSchema(id).orElse {
      val schema: Option[Schema] = connector.findSchema(id)
      schema.foreach(s => _cache.set(Some(cache.insert(Seq(getId(s) -> s)))))
      schema
    }
  }

  override def getAll: Seq[(Long, Schema)] = {
    _cache.set(Some(cache.insert(connector.fullLoad())))
    cache.getAll
  }
}
