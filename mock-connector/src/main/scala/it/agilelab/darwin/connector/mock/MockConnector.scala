package it.agilelab.darwin.connector.mock

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import it.agilelab.darwin.connector.mock.testclasses.{MockClassAlone, MockClassParent}
import org.apache.avro.Schema

import scala.collection.mutable

class MockConnector(config: Config) extends Connector(config) {

  val _table: mutable.Map[Long, Schema] = {
    val alone = new SchemaGenerator[MockClassAlone].schema
    val parent = new SchemaGenerator[MockClassParent].schema
    mutable.Map(0L -> alone, 1L -> parent)
  }

  override def fullLoad(): Seq[(Long, Schema)] = _table.toSeq

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    schemas.foreach { case(id, schema) =>
      _table(id) = schema
    }
  }

}
