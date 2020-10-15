package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import it.agilelab.darwin.common.{ Connector, ConnectorCreator, Logging }

import scala.collection.JavaConverters._

class ConfluentConnectorCreator extends ConnectorCreator with Logging {

  override def create(config: Config): Connector = {
    log.debug("creating confluent connector")

    val confluentOptions = ConfluentConnectorOptions.fromConfig(config)
    log.info("confluent options are {}", confluentOptions)

    val client = new CachedSchemaRegistryClient(
      confluentOptions.endpoints.asJava,
      confluentOptions.maxCachedSchemas,
      confluentOptions.config.asJava
    )

    val rest = new ConfluentConnector(confluentOptions, client)
    log.debug("created confluent connector")
    rest
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "confluent"
}
