package it.agilelab.darwin.connector.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema
import scalaj.http.Http

class RestConnector(options: RestConnectorOptions, config: Config) extends Connector with JsonProtocol {

  override def fullLoad(): Seq[(Long, Schema)] =
    Http(options.endpoint("schemas/")).execute(toSeqOfIdSchema).body

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {

    val response = Http(options.endpoint("schemas/"))
      .header("Content-Type", "application/json")
      .postData(toJson(schemas))
      .asString

    if (response.isError) {
      throw new Exception(response.body)
    }

  }

  override def createTable(): Unit = {}

  override def tableExists(): Boolean          = true

  override def tableCreationHint(): String = ""

  override def findSchema(id: Long): Option[Schema] = {

    val response = Http(options.endpoint(s"schemas/$id")).execute(toSchema)

    if (response.code == 404) {
      None
    } else {
      Some(response.body)
    }
  }
}
