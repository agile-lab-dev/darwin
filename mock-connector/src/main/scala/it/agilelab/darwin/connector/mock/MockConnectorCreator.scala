package it.agilelab.darwin.connector.mock

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class MockConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new MockConnector(config)

  /**
    * @return the name of the Connector
    */
  override def name(): String = "mock"
}
