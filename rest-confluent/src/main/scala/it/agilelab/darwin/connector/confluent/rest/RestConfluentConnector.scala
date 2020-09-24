package it.agilelab.darwin.connector.confluent.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, Logging}
import org.apache.avro.Schema
import scalaj.http.{Http}
import scala.util.{Failure, Success, Try}

class RestConfluentConnector(options: RestConfluentConnectorOptions, config: Config)
  extends Connector with JsonProtocol with Logging {

  override def fullLoad(): Seq[(Long, Schema)] = {
    throw new Exception("Method 'fullLoad' not implemented.")
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    throw new Exception("Method 'insert' not implemented.")
  }

  override def createTable(): Unit = {}

  override def tableExists(): Boolean = true

  override def tableCreationHint(): String = ""

  override def findSchema(id: Long): Option[Schema] = {
    retrieveSchema(options.endpoint(s"schemas/ids/$id"))
  }

  private def retrieveSchema(endpoint: String): Option[Schema] = {

    Try {
      Http(endpoint)
        .header("Accept", "application/vnd.schemaregistry.v1+json").execute(toSchema)
    } match {
      case Failure(exception) =>
        log.error("Error in retrieving or parsing schema from Confluent", exception)
        None
      case Success(value) if value.is2xx => Some(value.body)
      case _ =>
        log.error("Response code is not 200.")
        None
    }
  }

  def findSchema(subject: String, version: String): Option[Schema] = {
    retrieveSchema(options.endpoint(s"subjects/$subject/versions/$version"))
  }
}
