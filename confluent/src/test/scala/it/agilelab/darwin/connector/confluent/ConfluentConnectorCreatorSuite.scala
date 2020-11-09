package it.agilelab.darwin.connector.confluent

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConfluentConnectorCreatorSuite extends AnyFlatSpec with Matchers {

  "connector" should "create an instance" in {

    val conf: Config = ConfigFactory.parseString("""
                                                   | endpoints: ["endpoint-one", "endpoint-two"]
                                                   | max-cached-schemas: 1000
                                                   |
                                                   | kafka.schemaregistry.other: 1
                                                   | kafka.schemaregistry: {
                                                   |   other2: "stringa"
                                                   | }
                                                   |""".stripMargin)

    val connector = new ConfluentConnectorCreator()

    val options = ConfluentConnectorOptions.fromConfig(conf)

    val result = connector.create(conf)

    assert(result != null)

    val endpoints = options.config.get("endpoints").asInstanceOf[java.util.List[String]]

    endpoints.get(0) should be("endpoint-one")
    endpoints.get(1) should be("endpoint-two")

    options.config.get("kafka.schemaregistry.other").asInstanceOf[Int] should be(1)
    options.config.get("kafka.schemaregistry.other2").asInstanceOf[String] should be("stringa")

    val maxCached = 1000
    options.config.get("max-cached-schemas").asInstanceOf[Int] should be(maxCached)

  }

}
