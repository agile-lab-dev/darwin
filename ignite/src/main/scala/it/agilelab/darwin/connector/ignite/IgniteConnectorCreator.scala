package it.agilelab.darwin.connector.ignite

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class IgniteConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new IgniteConnector(config)
}
