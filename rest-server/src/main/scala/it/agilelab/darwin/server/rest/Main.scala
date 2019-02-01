package it.agilelab.darwin.server.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.connector.mock.MockConnector
import it.agilelab.darwin.manager.LazyAvroSchemaManager
import org.apache.avro.{Schema, SchemaBuilder}

object Main {




  def main(args: Array[String]): Unit = {

    implicit val actorSystem: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()


    val connector = new MockConnector(ConfigFactory.load())
    val schemaManager = new LazyAvroSchemaManager(connector)

    val schema = SchemaBuilder.array().items(Schema.create(Schema.Type.INT))

    schemaManager.registerAll(Seq(schema))

    HttpApp(DarwinService(schemaManager)).run()

  }
}
