package it.agilelab.darwin.manager

import java.nio.ByteOrder

import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

/**
  * Implementation of AvroSchemaManager that performs all the operations directly on the storage (retrievals and
  * insertions).
  */
class LazyAvroSchemaManager(connector: Connector, endianness: ByteOrder)
  extends AvroSchemaManager(connector, endianness) {

  def this(connector: Connector) = this(connector, ByteOrder.BIG_ENDIAN)

  override def getSchema(id: Long): Option[Schema] = connector.findSchema(id)

  override def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)] = {
    val schemasWithIds = schemas.map(s => getId(s) -> s)
    connector.insert(schemasWithIds)
    schemasWithIds
  }

  override def reload(): AvroSchemaManager = this

  override def getAll: Seq[(Long, Schema)] = connector.fullLoad()
}
