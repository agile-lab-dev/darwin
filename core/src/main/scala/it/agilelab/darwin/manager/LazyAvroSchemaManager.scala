package it.agilelab.darwin.manager

import com.typesafe.config.Config
import org.apache.avro.Schema

/**
  * Implementation of AvroSchemaManager that performs all the operations directly on the storage (retrievals and
  * insertions).
  */
case class LazyAvroSchemaManager private[darwin](override val config: Config) extends AvroSchemaManager {

  override def getSchema(id: Long): Option[Schema] = connector.findSchema(id)

  override def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)] = {
    val schemasWithIds = schemas.map(s => getId(s) -> s)
    connector.insert(schemasWithIds)
    schemasWithIds
  }

  override def reload(): AvroSchemaManager = this
}
