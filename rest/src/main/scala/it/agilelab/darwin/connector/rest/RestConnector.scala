package it.agilelab.darwin.connector.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

class RestConnector(config: Config) extends Connector(config) {
  override def fullLoad(): Seq[(Long, Schema)] = ???

  override def insert(schemas: Seq[(Long, Schema)]): Unit = ???

  override def createTable(): Unit = ???

  override def tableExists(): Boolean = ???

  override def tableCreationHint(): String = ???

  override def findSchema(id: Long): Option[Schema] = ???
}
