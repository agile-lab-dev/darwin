package it.agilelab.darwin.manager

import com.typesafe.config.Config
import org.apache.avro.Schema

/**
  * Implementation of CachedAvroSchemaManager that loads all the schemas into the cache at startup and doesn't
  * perform any other accesses to the storage: each retrieve is performed onto the cache.
  */
case class CachedEagerAvroSchemaManager private[darwin](override val config: Config) extends CachedAvroSchemaManager {

  override def getSchema(id: Long): Option[Schema] = cache.getSchema(id)
}