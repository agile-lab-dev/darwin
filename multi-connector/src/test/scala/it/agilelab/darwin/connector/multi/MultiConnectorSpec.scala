package it.agilelab.darwin.connector.multi

import com.typesafe.config.ConfigFactory
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import it.agilelab.darwin.common.ConnectorFactory
import it.agilelab.darwin.connector.confluent.{ConfluentConnector, ConfluentConnectorOptions}
import it.agilelab.darwin.connector.mock.{ConfigurationKeys, MockConnector, MockConnectorCreator}
import it.agilelab.darwin.manager.LazyAvroSchemaManager
import org.apache.avro.SchemaBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Paths
import java.nio.{ByteBuffer, ByteOrder}
import java.util
import java.util.Collections

class MultiConnectorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  private val p = Paths
    .get(".")
    .resolve("mock-connector")
    .resolve("src")
    .resolve("test")
    .resolve("resources")
    .resolve("test")

  it should "start with mock and confluent-mock connector" in {
    val confluent       = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), 1000),
      client = new MockSchemaRegistryClient()
    )
    val mock            = new MockConnectorCreator().create(ConfigFactory.parseMap {
      new java.util.HashMap[String, Object] {
        put(
          ConfigurationKeys.FILES,
          util.Arrays.asList(
            p.resolve("DoesNotExists.avsc").toString,
            p.resolve("MockClassAlone.avsc").toString,
            p.resolve("MockClassParent.avsc").toString
          )
        )
        put(ConfigurationKeys.MODE, "permissive")
      }
    })
    val multiC          = new MultiConnector(
      confluent,
      List(confluent),
      List(mock)
    )
    val initiallyLoaded = multiC.fullLoad()
    initiallyLoaded.size shouldBe 2
    val schemaToInsert  = SchemaBuilder
      .record("Test")
      .prop("x-darwin-subject", "test-value")
      .fields()
      .requiredLong("numero")
      .endRecord()
    val manager         = new LazyAvroSchemaManager(multiC, ByteOrder.BIG_ENDIAN)
    val id              = manager.registerAll(Seq(schemaToInsert)).head._1
    multiC.fullLoad().size shouldBe 3
    val parsedId        = manager.extractId(
      Array(0x00: Byte) ++
        ByteBuffer.wrap(Array.ofDim[Byte](4)).putInt(id.toInt).array()
    )
    parsedId shouldBe id
    initiallyLoaded.foreach { case (id, schema) =>
      manager.extractId(
        mock.generateAvroSingleObjectEncoded(Array.emptyByteArray, schema, ByteOrder.BIG_ENDIAN, mock.fingerprint _)
      ) shouldBe id
    }
  }

  it should "create with the creator" in {
    val multiConnectorCreator = ConnectorFactory.creator("multi").get
    val connector: MultiConnector = multiConnectorCreator.create(
      ConfigFactory.parseString(
        s"""
           |  type = "eager"
           |  connector = "multi"
           |  registrator = "mock"
           |  confluent-single-object-encoding: ["confluent"]
           |  standard-single-object-encoding: ["mock"]
           |  confluent {
           |    endpoints: ["http://schema-registry-00:7777", "http://schema-registry-01:7777"]
           |    max-cached-schemas: 1000
           |  }
           |  mock {
           |    ${ConfigurationKeys.FILES} = [
           |      ${p.resolve("DoesNotExists.avsc").toString},
           |      ${p.resolve("MockClassAlone.avsc").toString},
           |      ${p.resolve("MockClassParent.avsc").toString}
           |    ]
           |    ${ConfigurationKeys.MODE} = "permissive"
           |  }
           |""".stripMargin
      )
    ).asInstanceOf[MultiConnector]
    connector.registrator.isInstanceOf[ConfluentConnector]
    connector.confluentConnectors.head.isInstanceOf[ConfluentConnector]
    connector.singleObjectEncodingConnectors.head.isInstanceOf[MockConnector]
  }
}
