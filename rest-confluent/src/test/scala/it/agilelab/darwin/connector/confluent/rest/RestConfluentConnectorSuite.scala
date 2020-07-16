package it.agilelab.darwin.connector.confluent.rest

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec

class RestConfluentConnectorSuite extends AnyFlatSpec {

  private def config() = ConfigFactory.parseString(
    s"""
       | protocol: "http"
       | host: "localhost"
       | port: "8081"
       | basePath: "/"
      """.stripMargin)

  "rest confluent connector" should "correctly retrieve the schema associated to the provided id" in {
    val connector = new RestConfluentConnectorCreator().create(config)

    val c = connector.findSchema(6)
    println(c.get.toString)
  }

}
