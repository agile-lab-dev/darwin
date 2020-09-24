package it.agilelab.darwin.connector.confluent.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, getRequestedFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.typesafe.config.ConfigFactory
import org.apache.avro.Schema
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}

class RestConfluentConnectorSuite extends AnyFlatSpec with BeforeAndAfterEach with OptionValues {

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  override def beforeEach: Unit = {
    wireMockServer.start()
  }

  override def afterEach: Unit = {
    wireMockServer.stop()
  }

  val parser = new Schema.Parser
  val expected: Schema = parser.parse("""{"type":"record","name":"Envelope","namespace":"dbserver1.inventory.customers","fields":[{"name":"before","type":["null",{"type":"record","name":"Value","fields":[{"name":"id","type":"int"},{"name":"first_name","type":"string"},{"name":"last_name","type":"string"},{"name":"email","type":"string"}],"connect.name":"dbserver1.inventory.customers.Value"}],"default":null},{"name":"after","type":["null","Value"],"default":null},{"name":"source","type":{"type":"record","name":"Source","namespace":"io.debezium.connector.mysql","fields":[{"name":"name","type":"string"},{"name":"server_id","type":"long"},{"name":"ts_sec","type":"long"},{"name":"gtid","type":["null","string"],"default":null},{"name":"file","type":"string"},{"name":"pos","type":"long"},{"name":"row","type":"int"},{"name":"snapshot","type":["null","boolean"],"default":null},{"name":"thread","type":["null","long"],"default":null},{"name":"db","type":["null","string"],"default":null},{"name":"table","type":["null","string"],"default":null}],"connect.name":"io.debezium.connector.mysql.Source"}},{"name":"op","type":"string"},{"name":"ts_ms","type":["null","long"],"default":null}],"connect.version":1,"connect.name":"dbserver1.inventory.customers.Envelope"}""")


  private def config(port: Int) = ConfigFactory.parseString(
    s"""
       | protocol: "http"
       | host: "localhost"
       | port: ${wireMockServer.port()}
       | basePath: "/"
      """.stripMargin)


  "Rest Confluent Connector" should "get a specific schema." in {

    val subject = "users-value"
    val version = "latest"
    val connector = new RestConfluentConnectorCreator()
      .create(config(wireMockServer.port())).asInstanceOf[RestConfluentConnector]
    wireMockServer.stubFor {
      get(urlPathEqualTo(s"/subjects/$subject/versions/$version")).willReturn {
        aResponse().withBody {
          """
            | {
            |    "schema": "{\"type\":\"record\",\"name\":\"Envelope\",\"namespace\":\"dbserver1.inventory.customers\",\"fields\":[{\"name\":\"before\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"Value\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"first_name\",\"type\":\"string\"},{\"name\":\"last_name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"}],\"connect.name\":\"dbserver1.inventory.customers.Value\"}],\"default\":null},{\"name\":\"after\",\"type\":[\"null\",\"Value\"],\"default\":null},{\"name\":\"source\",\"type\":{\"type\":\"record\",\"name\":\"Source\",\"namespace\":\"io.debezium.connector.mysql\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"server_id\",\"type\":\"long\"},{\"name\":\"ts_sec\",\"type\":\"long\"},{\"name\":\"gtid\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"file\",\"type\":\"string\"},{\"name\":\"pos\",\"type\":\"long\"},{\"name\":\"row\",\"type\":\"int\"},{\"name\":\"snapshot\",\"type\":[\"null\",\"boolean\"],\"default\":null},{\"name\":\"thread\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"db\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"table\",\"type\":[\"null\",\"string\"],\"default\":null}],\"connect.name\":\"io.debezium.connector.mysql.Source\"}},{\"name\":\"op\",\"type\":\"string\"},{\"name\":\"ts_ms\",\"type\":[\"null\",\"long\"],\"default\":null}],\"connect.version\":1,\"connect.name\":\"dbserver1.inventory.customers.Envelope\"}"
            | }
          """.stripMargin
        }
      }
    }
    val result = connector.findSchema("users-value", "latest").value

    assert(result == expected)

    wireMockServer.verify {
      getRequestedFor(urlPathEqualTo(s"/subjects/$subject/versions/$version"))
    }

  }

  it should "return None when Confluent's response could not be parsed as Schema" in {
    val subject = "users-value"
    val version = "latest"
    val connector = new RestConfluentConnectorCreator()
      .create(config(wireMockServer.port())).asInstanceOf[RestConfluentConnector]
    wireMockServer.stubFor {
      get(urlPathEqualTo(s"/subjects/$subject/versions/$version")).willReturn {
        aResponse().withBody {
          """
            | {
            |    "schema": "NOT-A-SCHEMA"
            | }
          """.stripMargin
        }
      }
    }
    val result = connector.findSchema("users-value", "latest")

    assert(result.isEmpty)

  }

  it should "return None when the request is not correctly formulated." in {
    val subject = "users-value"
    val version = "latest"
    val connector = new RestConfluentConnectorCreator()
      .create(config(wireMockServer.port())).asInstanceOf[RestConfluentConnector]
    wireMockServer.stubFor {
      get(urlPathEqualTo(s"/subjects/$subject/versions/$version")).willReturn {
        aResponse().withBody {
          """
            | {
            |    "schema": "{\"type\":\"record\",\"name\":\"Envelope\",\"namespace\":\"dbserver1.inventory.customers\",\"fields\":[{\"name\":\"before\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"Value\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"first_name\",\"type\":\"string\"},{\"name\":\"last_name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":\"string\"}],\"connect.name\":\"dbserver1.inventory.customers.Value\"}],\"default\":null},{\"name\":\"after\",\"type\":[\"null\",\"Value\"],\"default\":null},{\"name\":\"source\",\"type\":{\"type\":\"record\",\"name\":\"Source\",\"namespace\":\"io.debezium.connector.mysql\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"server_id\",\"type\":\"long\"},{\"name\":\"ts_sec\",\"type\":\"long\"},{\"name\":\"gtid\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"file\",\"type\":\"string\"},{\"name\":\"pos\",\"type\":\"long\"},{\"name\":\"row\",\"type\":\"int\"},{\"name\":\"snapshot\",\"type\":[\"null\",\"boolean\"],\"default\":null},{\"name\":\"thread\",\"type\":[\"null\",\"long\"],\"default\":null},{\"name\":\"db\",\"type\":[\"null\",\"string\"],\"default\":null},{\"name\":\"table\",\"type\":[\"null\",\"string\"],\"default\":null}],\"connect.name\":\"io.debezium.connector.mysql.Source\"}},{\"name\":\"op\",\"type\":\"string\"},{\"name\":\"ts_ms\",\"type\":[\"null\",\"long\"],\"default\":null}],\"connect.version\":1,\"connect.name\":\"dbserver1.inventory.customers.Envelope\"}"
            | }
          """.stripMargin
        }
      }
    }
    val result = connector.findSchema("users-value-WRONG", "latest")

    assert(result.isEmpty)

    wireMockServer.verify {
      getRequestedFor(urlPathEqualTo(s"/subjects/$subject/versions/$version"))
    }

  }

}
