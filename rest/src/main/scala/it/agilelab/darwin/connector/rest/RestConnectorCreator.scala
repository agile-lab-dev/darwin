package it.agilelab.darwin.connector.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class RestConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new RestConnector(config)

  /**
    * @return the name of the Connector
    */
  override def name(): String = "rest"
}
