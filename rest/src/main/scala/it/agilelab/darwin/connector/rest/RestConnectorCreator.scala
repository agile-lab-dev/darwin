package it.agilelab.darwin.connector.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.{ Connector, ConnectorCreator, Logging }

class RestConnectorCreator extends ConnectorCreator with Logging {

  override def create(config: Config): Connector = {
    log.debug("creating rest connector")

    val restOptions = RestConnectorOptions.fromConfig(config)
    log.info("rest options are {}", restOptions)

    val rest = new RestConnector(restOptions, config)
    log.debug("created rest connector")
    rest
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "rest"
}
