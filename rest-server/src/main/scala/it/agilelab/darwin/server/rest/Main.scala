package it.agilelab.darwin.server.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.manager.AvroSchemaManagerFactory

object Main {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config = ConfigFactory.load()
    val schemaManager = AvroSchemaManagerFactory.initialize(config.getConfig("darwin"))

    HttpApp(DarwinService(schemaManager)).run()

  }
}
