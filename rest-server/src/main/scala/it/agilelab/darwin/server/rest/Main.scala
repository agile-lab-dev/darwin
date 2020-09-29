package it.agilelab.darwin.server.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.manager.AvroSchemaManagerFactory

object Main {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem        = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val config              = ConfigFactory.load()
    val schemaManagerConfig = config.getConfig("darwin")
    val restConfig          = config.getConfig("darwin-rest")
    val schemaManager       = AvroSchemaManagerFactory.initialize(schemaManagerConfig)

    HttpApp(restConfig, DarwinService(schemaManager)).run()

  }
}
