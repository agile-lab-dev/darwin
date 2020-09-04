package it.agilelab.darwin.connector.confluent.rest

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator, Logging}

class RestConfluentConnectorCreator extends ConnectorCreator with Logging {

  override def create(config: Config): Connector = {
    log.debug("creating rest confluent connector")

    val restOptions = RestConfluentConnectorOptions.fromConfig(config)
    log.info("rest options are {}", restOptions)

    val rest = new RestConfluentConnector(restOptions, config)
    log.debug("created rest confluent connector")
    rest
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "rest-confluent"
}



