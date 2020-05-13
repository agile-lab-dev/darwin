package it.agilelab.darwin.connector.postgres

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class PostgresConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new PostgresConnector(config)

  /**
    * @return the name of the Connector
    */
  override def name(): String = "postgresql"
}
