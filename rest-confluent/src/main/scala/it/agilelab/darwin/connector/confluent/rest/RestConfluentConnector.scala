package it.agilelab.darwin.connector.confluent.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema
import scalaj.http.{Http, HttpResponse}

class RestConfluentConnector(options: RestConfluentConnectorOptions, config: Config)
  extends Connector with JsonProtocol {

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

  def findSchema(subject: String, version: String): Option[Schema] = {
    retrieveSchema(options.endpoint(s"subjects/$subject/versions/$version"))
  }

  private def retrieveSchema(endpoint: String) = {
    val response: HttpResponse[Schema] = Http(endpoint)
      .header("Accept", "application/vnd.schemaregistry.v1+json").execute(toSchema)

    response.code match {
      case 200 => Some(response.body)
      case _ => None
    }
  }

}
