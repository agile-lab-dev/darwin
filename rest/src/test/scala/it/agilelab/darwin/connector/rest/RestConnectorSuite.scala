package it.agilelab.darwin.connector.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.typesafe.config.ConfigFactory
import org.apache.avro.{ Schema, SchemaBuilder }
import org.scalatest.{ BeforeAndAfterEach, OptionValues }
import org.scalatest.flatspec.AnyFlatSpec

class RestConnectorSuite extends AnyFlatSpec with BeforeAndAfterEach with OptionValues {

  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

  private def config(port: Int) = ConfigFactory.parseString(s"""
                                                               | protocol: "http"
                                                               | host: "localhost"
                                                               | port: ${wireMockServer.port()}
                                                               | basePath: "/"
      """.stripMargin)

  override def beforeEach(): Unit =
    wireMockServer.start()

  override def afterEach(): Unit =
    wireMockServer.stop()

  "rest connector" should "get all schemas" in {

    val connector = new RestConnectorCreator().create(config(wireMockServer.port()))

    val schemaId1 = -3577210133426481249L
    val schemaId2 = 5920968314789803198L

    wireMockServer.stubFor {
      get(urlPathEqualTo("/schemas/")).willReturn {
        aResponse().withBody {
          s"""
             |[{
             |  "id": "$schemaId1",
             |  "schema": {
             |    "items": "string",
             |    "type": "array"
             |  }
             | }, {
             |  "id": "$schemaId2",
             |  "schema": {
             |    "items": "int",
             |    "type": "array"
             |  }
             | }]
          """.stripMargin
        }
      }
    }

    val result = connector.fullLoad()

    assert(result.contains((schemaId1, SchemaBuilder.array().items(Schema.create(Schema.Type.STRING)))))
    assert(result.contains((schemaId2, SchemaBuilder.array().items(Schema.create(Schema.Type.INT)))))
    assert(result.size == 2)

    wireMockServer.verify {
      getRequestedFor(urlPathEqualTo("/schemas/"))
    }

  }

  "rest connector" should "get one schemas" in {

    val schemaId  = -3577210133426481249L
    val connector = new RestConnectorCreator().create(config(wireMockServer.port()))

    wireMockServer.stubFor {
      get(urlPathEqualTo(s"/schemas/$schemaId")).willReturn {
        aResponse().withBody {
          """
            | {
            |    "items": "string",
            |    "type": "array"
            | }
          """.stripMargin
        }
      }
    }

    val result = connector.findSchema(schemaId).value

    val expected = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))

    assert(result == expected)

    wireMockServer.verify {
      getRequestedFor(urlPathEqualTo(s"/schemas/$schemaId"))
    }

  }

  "rest connector" should "post schemas" in {
    val connector = new RestConnectorCreator().create(config(wireMockServer.port()))

    val schema = SchemaBuilder.array().items(Schema.create(Schema.Type.INT))

    wireMockServer.stubFor {
      post(urlEqualTo("/schemas/")).withHeader("Content-Type", equalTo("application/json"))
    }

    connector.insert(Seq((0, schema)))

    val request = """[{"type":"array","items":"int"}]"""

    wireMockServer.verify {
      postRequestedFor(urlEqualTo("/schemas/")).withRequestBody(equalTo(request))
    }

  }
}
