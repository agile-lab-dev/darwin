package it.agilelab.darwin.connector.dynamo

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class DynamoConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = ???

  /**
    * @return the name of the Connector
    */
  override def name(): String = "dynamo"
}
