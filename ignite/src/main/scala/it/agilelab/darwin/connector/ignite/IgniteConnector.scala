package it.agilelab.darwin.connector.ignite

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

class IgniteConnector(config: Config) extends Connector {
  override def fullLoad(): Seq[(Long, Schema)] = ???

  override def insert(schemas: Seq[(Long, Schema)]): Unit = ???

  override def findSchema(id: Long): Option[Schema] = ???

  override def createTable(): Unit = ???

  override def tableExists(): Boolean = ???

  override def tableCreationHint(): String = ???

  override def findSchemasByName(name: String): Seq[Schema] = ???

  override def findSchemasByNamespace(namespace: String): Seq[Schema] = ???

  override def findSchemaByNameAndNamespace(name: String, namespace: String): Seq[Schema] = ???
}
