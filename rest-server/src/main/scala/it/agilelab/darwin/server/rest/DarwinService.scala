package it.agilelab.darwin.server.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.ActorMaterializer
import akka.stream.Attributes.LogLevels
import it.agilelab.darwin.manager.AvroSchemaManager
import org.apache.avro.Schema

trait DarwinService extends Service with Directives with DebuggingDirectives with JsonSupport {

  val manager: AvroSchemaManager

  override def route: Route = logRequestResult(("darwin", LogLevels.Debug)) {
    get {
      path("schemas" / LongNumber.?) {
        case Some(id) =>
          manager.getSchema(id) match {
            case Some(schema) => complete(schema)
            case None         =>
              complete {
                HttpResponse(StatusCodes.NotFound)
              }
          }
        case None     => complete(manager.getAll)
      }
    } ~ post {
      path("schemas" / PathEnd) {
        entity(as[Seq[Schema]]) { schemas =>
          complete {
            manager.registerAll(schemas).map(_._1)
          }
        }
      }
    }
  }
}

object DarwinService {
  def apply(asm: AvroSchemaManager)(implicit s: ActorSystem, m: ActorMaterializer): DarwinService = new DarwinService {
    implicit override val materializer: ActorMaterializer = m
    implicit override val system: ActorSystem             = s
    override val manager: AvroSchemaManager               = asm
  }
}
