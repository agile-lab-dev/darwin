package it.agilelab.darwin.connector.confluent

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import org.apache.avro.{ Schema, SchemaBuilder }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterEach, OptionValues }

class ConfluentConnectorSuite extends AnyFlatSpec with BeforeAndAfterEach with OptionValues with Matchers {

  "confluent connector" should "insert schemas and generate ids" in {

    val mockRegistryClient = new MockSchemaRegistryClient()
    val maxCachedSchemas   = 1000
    val connector          = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Map.empty, maxCachedSchemas),
      client = mockRegistryClient
    )

    val expected = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))

    expected.addProp("x-darwin-subject", "prova")

    val id = connector.fingerprint(expected)

    connector.insert(Seq((id, expected)))

    connector.findSchema(id).value shouldBe (expected)

  }

  "confluent connector" should "be able to preload schemas" in {

    val expected  = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))
    expected.addProp("x-darwin-subject", "prova")
    val expected2 = SchemaBuilder.array().items(Schema.create(Schema.Type.INT))
    expected2.addProp("x-darwin-subject", "prova2")

    val mockRegistryClient = new MockSchemaRegistryClient()

    mockRegistryClient.register("prova", new AvroSchema(expected))
    mockRegistryClient.register("prova2", new AvroSchema(expected2))

    val maxCachedSchemas = 1000

    val connector = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Map.empty, maxCachedSchemas),
      client = mockRegistryClient
    )

    val fullLoaded = connector.fullLoad()

    fullLoaded should contain theSameElementsAs (Seq((1, expected), (2, expected2)))

  }
}
