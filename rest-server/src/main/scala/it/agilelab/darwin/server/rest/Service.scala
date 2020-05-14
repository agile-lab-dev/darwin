package it.agilelab.darwin.server.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

trait Service {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  def route: Route
}
