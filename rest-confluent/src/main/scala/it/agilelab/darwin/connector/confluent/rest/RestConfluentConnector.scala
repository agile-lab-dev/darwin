package it.agilelab.darwin.connector.confluent.rest

//import akka.actor.ActorSystem
//import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
//import akka.http.scaladsl.server.directives.DebuggingDirectives
//import akka.http.scaladsl.server.{Directives, PathMatcher, Route}
//import akka.stream.Attributes.LogLevels
//import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema
import scalaj.http.{Http, HttpResponse}

class RestConfluentConnector(options: RestConfluentConnectorOptions, config: Config)
  extends Connector with JsonProtocol {

  //with directives with debuggingdirectives
//  implicit val system = ActorSystem()
//  implicit val materializer = ActorMaterializer()

  override def fullLoad(): Seq[(Long, Schema)] = ???

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {

//    val response = Http(options.endpoint("schemas/"))
//      .header("Content-Type", "application/json")
//      .postData(toJson(schemas))
//      .asString
//
//    if (response.isError) {
//      throw new Exception(response.body)
//    }

  }

  override def createTable(): Unit = {}

  override def tableExists(): Boolean = true

  override def tableCreationHint(): String = ""

  override def findSchema(id: Long): Option[Schema] = {

    val response: HttpResponse[Schema] = Http(options.endpoint(s"schemas/ids/$id"))
      .header("Accept", "application/vnd.schemaregistry.v1+json").execute(toSchema)

    if (response.code == 404) {
      None
    } else {
      Some(response.body)
    } match {
      case Some(schema: Schema) => Some(schema)
      case _ => None
    }
  }

}
