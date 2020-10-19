package it.agilelab.darwin.connector.confluent

import java.util.Collections

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import it.agilelab.darwin.common.compat._
import org.apache.avro.{ Schema, SchemaBuilder }

object Main {
  def main(args: Array[String]): Unit = {
    // to run this main https://github.com/confluentinc/cp-all-in-one/blob/6.0.0-post/cp-all-in-one/docker-compose.yml

    val maxSchemas = 1000
    val options    = ConfluentConnectorOptions(List("http://localhost:8081"), Collections.emptyMap(), maxSchemas)

    val client    = new CachedSchemaRegistryClient(
      options.endpoints.toJavaList(),
      options.maxCachedSchemas,
      options.config
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

    expected.addProp("x-darwin-subject", "prova2-value": AnyRef)

    val id = connector.fingerprint(expected)

    connector.insert(Seq((id, expected)))

    connector.fullLoad().foreach(println)
  }
}
