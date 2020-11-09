package it.agilelab.darwin.connector.confluent

import java.util.Collections

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
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), maxCachedSchemas),
      client = mockRegistryClient
    )

    val expected = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))
    expected.addProp("x-darwin-subject", "prova": AnyRef)

    val id = connector.fingerprint(expected)

    connector.insert(Seq((id, expected)))

    connector.findSchema(id).value shouldBe expected

  }

  "confluent connector" should "be able to preload schemas" in {

    val expected  = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))
    expected.addProp("x-darwin-subject", "prova": AnyRef)
    val expected2 = SchemaBuilder.array().items(Schema.create(Schema.Type.INT))
    expected2.addProp("x-darwin-subject", "prova2": AnyRef)

    val mockRegistryClient = new MockSchemaRegistryClient()

    mockRegistryClient.register("prova", expected)
    mockRegistryClient.register("prova2", expected2)

    val maxCachedSchemas = 1000

    val connector = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), maxCachedSchemas),
      client = mockRegistryClient
    )

    val fullLoaded = connector.fullLoad()

    fullLoaded should contain theSameElementsAs Seq((1, expected), (2, expected2))

  }

  "confluent connector" should "be able to fetch latest schema for subject" in {

    val expected = SchemaBuilder
      .record("record")
      .fields()
      .requiredString("stringField")
      .endRecord()

    val expected2 = SchemaBuilder
      .record("record")
      .fields()
      .requiredString("stringField")
      .nullableString("stringField2", "default-for-nullable")
      .endRecord()

    expected.addProp("x-darwin-subject", "prova": AnyRef)
    expected2.addProp("x-darwin-subject", "prova": AnyRef)

    val mockRegistryClient = new MockSchemaRegistryClient()

    mockRegistryClient.register("prova", expected)
    mockRegistryClient.register("prova", expected2)

    val maxCachedSchemas = 1000

    val connector = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), maxCachedSchemas),
      client = mockRegistryClient
    )

    val fullLoaded = connector.fullLoad()

    fullLoaded should contain theSameElementsAs Seq((1, expected), (2, expected2))

    val latestResult = connector.findIdForSubjectLatestVersion("prova")

    val allVersions = connector.findVersionsForSubject("prova")

    val parser = (schema: String) => new Schema.Parser().parse(schema)

    val versionsByVersionId = allVersions
      .map(x => connector.findIdForSubjectVersion("prova", x))
      .map(x => x.getId -> parser(x.getSchema))

    latestResult.getId should be(2)

    versionsByVersionId should contain theSameElementsAs Seq((1, expected), (2, expected2))

  }

  "confluent connector" should "detect a missing x-darwin-subject" in {
    val expected = SchemaBuilder.array().items(Schema.create(Schema.Type.STRING))

    val mockRegistryClient = new MockSchemaRegistryClient()

    mockRegistryClient.register("prova", expected)

    val maxCachedSchemas = 1000

    val connector = new ConfluentConnector(
      options = ConfluentConnectorOptions(List.empty, Collections.emptyMap(), maxCachedSchemas),
      client = mockRegistryClient
    )

    val exception = intercept[IllegalArgumentException] {
      connector.insert(Seq(expected).map(schema => connector.fingerprint(schema) -> schema))
    }

    exception.getMessage should be("Schema does not contain the [x-darwin-subject] extension")

  }
}
