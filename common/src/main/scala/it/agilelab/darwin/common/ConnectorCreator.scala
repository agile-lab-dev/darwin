package it.agilelab.darwin.common

import com.typesafe.config.Config

/**
  * A generic interface used to create the [[Connector]] found in the classpath.
  */
trait ConnectorCreator {

  /**
    * This method should be overridden in each connector module returning its implementation.
    *
    * @param config configuration that will be used to create the correct implementation of [[Connector]]
    * @return the specific instance of [[Connector]]
    */
  def create(config: Config): Connector
}
