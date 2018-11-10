package it.agilelab.darwin.manager

import com.typesafe.config.Config
import org.apache.avro.Schema

/**
  * The main entry point to this library.
  * N.B.: each method all on this object must always be AFTER the initialization, performed invoking the initialize
  * method.
  */
case class CachedEagerAvroSchemaManager(override val config: Config) extends CachedAvroSchemaManager {

  override def getId(schema: Schema): Long = cache.getId(schema)

  override def getSchema(id: Long): Schema = cache.getSchema(id)
}