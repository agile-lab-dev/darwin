package it.agilelab.darwin.manager.exception

import com.typesafe.config.Config
import it.agilelab.darwin.manager.util.ConfigUtil

class ConnectorNotFoundException(val config: Config) extends RuntimeException(s"Cannot find Darwin connector") {

  def confAsString(): String = ConfigUtil.printConfig(config)

}
