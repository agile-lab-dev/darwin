package it.agilelab.darwin.connector.mysql

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class MySQLConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new MySQLConnector(config)
}
