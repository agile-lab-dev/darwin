package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{ Connector, ConnectorCreator, Logging }

class ConfluentConnectorCreator extends ConnectorCreator with Logging {

  override def create(config: Config): Connector = {
    log.debug("creating confluent connector")

    val confluentOptions = ConfluentConnectorOptions.fromConfig(config)
    log.info("confluent options are {}", confluentOptions)

    val client = new CachedSchemaRegistryClient(
      confluentOptions.endpoints.toJavaList(),
      confluentOptions.maxCachedSchemas,
      confluentOptions.config
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
