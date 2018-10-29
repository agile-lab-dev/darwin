package it.agilelab.darwin.connector.mongo

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator}

class MongoConnectorCreator extends ConnectorCreator {
  override def create(config: Config): Connector = new MongoConnector(config)
}
