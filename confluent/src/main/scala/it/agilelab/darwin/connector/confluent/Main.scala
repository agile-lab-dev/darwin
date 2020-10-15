package it.agilelab.darwin.connector.confluent

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.apache.avro.{ Schema, SchemaBuilder }

import scala.collection.JavaConverters._

object Main {
  def main(args: Array[String]): Unit = {
    // to run this main https://github.com/confluentinc/cp-all-in-one/blob/6.0.0-post/cp-all-in-one/docker-compose.yml

    val maxSchemas = 1000
    val options    = ConfluentConnectorOptions(List("http://localhost:8081"), Map.empty, maxSchemas)

    val client    = new CachedSchemaRegistryClient(
      options.endpoints.asJava,
      options.maxCachedSchemas,
      options.config.asJava
    )
    val connector = new ConfluentConnector(options, client)

    connector.fullLoad().foreach(println)

    val expected: Schema = SchemaBuilder
      .record("myrecord")
      .namespace("it.agilelab.record")
      .fields()
      .requiredString("myfield")
      .optionalString("ciccio")
      .endRecord()

    expected.addProp("x-darwin-subject", "prova2-value")

    val id = connector.fingerprint(expected)

    connector.insert(Seq((id, expected)))

    connector.fullLoad().foreach(println)
  }
}
