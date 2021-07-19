package it.agilelab.darwin.connector.multi

import com.typesafe.config.ConfigFactory
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import it.agilelab.darwin.common.ConnectorFactory
import it.agilelab.darwin.connector.confluent.{ ConfluentConnector, ConfluentConnectorOptions }
import it.agilelab.darwin.connector.mock.{ ConfigurationKeys, MockConnector, MockConnectorCreator }
import it.agilelab.darwin.manager.LazyAvroSchemaManager
import org.apache.avro.SchemaBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.nio.file.Paths
import java.nio.{ ByteBuffer, ByteOrder }
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

  private def mockConnector() = {
    new MockConnectorCreator().create(ConfigFactory.parseMap {
      new util.HashMap[String, Object] {
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
  }

  private def mockConfluentConnector() = {
    new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), 1000),
      client = new MockSchemaRegistryClient()
    )
  }

  it should "start with mock and confluent-mock connector" in {
    val confluent = mockConfluentConnector
    val mock      = mockConnector
    val multiC    = new MultiConnector(
      confluent,
      Some(confluent),
      List(mock)
    )

    val initiallyLoaded = multiC.fullLoad()
    initiallyLoaded.size shouldBe 2
    initiallyLoaded.foreach { case (id, schema) =>
      multiC.extractId(
        mock.generateAvroSingleObjectEncoded(Array.emptyByteArray, schema, ByteOrder.BIG_ENDIAN, mock.fingerprint _),
        ByteOrder.BIG_ENDIAN
      ) shouldBe id
    }
  }

  it should "register a schema using the registrar" in {
    val confluent = mockConfluentConnector
    val mock      = mockConnector
    val multiC    = new MultiConnector(
      confluent,
      Some(confluent),
      List(mock)
    )

    val schemaToInsert = SchemaBuilder
      .record("Test")
      .prop("x-darwin-subject", "test-value")
      .fields()
      .requiredLong("numero")
      .endRecord()
    val manager        = new LazyAvroSchemaManager(multiC, ByteOrder.BIG_ENDIAN)
    val id             = manager.registerAll(Seq(schemaToInsert)).head._1
    multiC.fullLoad().size shouldBe 3
    val parsedId       = manager.extractId(
      Array(0x00: Byte) ++
        ByteBuffer.wrap(Array.ofDim[Byte](4)).putInt(id.toInt).array()
    )
    parsedId shouldBe id
  }

  it should "be created with a confluent connector and a mock one" in {
    val multiConnectorCreator     = ConnectorFactory.creator("multi").get
    val connector: MultiConnector = multiConnectorCreator
      .create(
        ConfigFactory.parseString(
          s"""
             |  type = "eager"
             |  connector = "multi"
             |  registrar = "confluent"
             |  confluent-single-object-encoding: "confluent"
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
      )
      .asInstanceOf[MultiConnector]
    assert(connector.registrar.isInstanceOf[ConfluentConnector])
    assert(connector.confluentConnector.exists(_.isInstanceOf[ConfluentConnector]))
    assert(connector.singleObjectEncodingConnectors.forall(_.isInstanceOf[MockConnector]))
  }

  it should "be created with only a mock connector" in {
    val multiConnectorCreator     = ConnectorFactory.creator("multi").get
    val connector: MultiConnector = multiConnectorCreator
      .create(
        ConfigFactory.parseString(
          s"""
             |  type = "eager"
             |  connector = "multi"
             |  registrar = "mock"
             |  standard-single-object-encoding: ["mock"]
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
      )
      .asInstanceOf[MultiConnector]
    connector.confluentConnector shouldBe empty
    assert(connector.registrar.isInstanceOf[MockConnector])
    assert(connector.singleObjectEncodingConnectors.forall(_.isInstanceOf[MockConnector]))
  }

  it should "extract schema and payload from confluent encoded byte array" in {
    val confluent      = mockConfluentConnector
    val mock           = mockConnector
    val multiC         = new MultiConnector(
      confluent,
      Some(confluent),
      List(mock)
    )
    val schemaToInsert = SchemaBuilder
      .record("Testa")
      .prop("x-darwin-subject", "test-value")
      .fields()
      .requiredLong("numera")
      .endRecord()
    val manager        = new LazyAvroSchemaManager(multiC, ByteOrder.BIG_ENDIAN)
    val id             = manager.registerAll(Seq(schemaToInsert)).head._1
    manager.extractId(
      manager.generateAvroSingleObjectEncoded(Array.emptyByteArray, schemaToInsert)
    ) shouldBe id

    manager.extractId(
      ByteBuffer.wrap(manager.generateAvroSingleObjectEncoded(Array.emptyByteArray, schemaToInsert))
    ) shouldBe id

    val stream = new ByteArrayOutputStream()
    manager.generateAvroSingleObjectEncoded(stream, id)(identity)
    manager.extractId(
      new ByteArrayInputStream(stream.toByteArray)
    ) shouldBe Right(id)

    manager.extractSchema(new ByteArrayInputStream(stream.toByteArray)) shouldBe Right(schemaToInsert)

    val soe = ByteBuffer.wrap(manager.generateAvroSingleObjectEncoded(Array.emptyByteArray, schemaToInsert))
    manager.retrieveSchemaAndAvroPayload(soe) shouldBe schemaToInsert

    manager.retrieveSchemaAndAvroPayload(
      ByteBuffer.wrap(manager.generateAvroSingleObjectEncoded(Array.emptyByteArray, schemaToInsert))
    ) shouldBe schemaToInsert

  }
}
