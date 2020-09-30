package it.agilelab.darwin.connector.hbase

import com.typesafe.config.Config
import it.agilelab.darwin.common.{ Connector, ConnectorCreator, Logging }

class HBaseConnectorCreator extends ConnectorCreator with Logging {
  override def create(config: Config): Connector = {
    log.debug("creating the HBase connector")
    val connector: Connector = HBaseConnector.instance(config)
    log.debug("HBase connector created")
    connector
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "hbase"
}
