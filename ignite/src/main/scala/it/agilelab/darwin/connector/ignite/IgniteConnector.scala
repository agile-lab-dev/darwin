package it.agilelab.darwin.connector.ignite

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

class IgniteConnector(config: Config) extends Connector(config) {
  override def fullLoad(): Seq[(Long, Schema)] = ???

  override def insert(schemas: Seq[(Long, Schema)]): Unit = ???

  override def findSchema(id: Long): Option[Schema] = ???
}
