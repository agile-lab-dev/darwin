package it.agilelab.darwin.common

import java.util.ServiceLoader

import com.typesafe.config.Config
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException
import it.agilelab.darwin.manager.util.ConfigurationKeys

import it.agilelab.darwin.common.compat._

/**
  * Used to obtain the correct implementation of [[Connector]] found on the classpath using the [[ConnectorCreator]]
  */
object ConnectorFactory extends Logging {

  /**
    * Retrieves all the registered [[ConnectorCreator]] in the classpath.
    *
    * @return a sequence of all the loaded [[ConnectorCreator]]
    */
  def creators(): Seq[ConnectorCreator] = {
    val creators = ServiceLoader.load(classOf[ConnectorCreator]).toScala.toSeq
    log.debug(s"${creators.size} available connector creators found")
    creators
  }

  /**
    * @return the first ConnectorCreator, use ONLY if you are sure that just one is available in the classpath
    */
  def creator(): Option[ConnectorCreator] = creators().headOption


  /**
    * @return the ConnectorCreator identified by the name given as input
    */
  def creator(name: String): Option[ConnectorCreator] = {
    creators().find(_.name == name)
  }

  /**
    * @return the ConnectorCreator identified by the name given as input
    */
  def creator(conf: Config): Option[ConnectorCreator] = {
    if (conf.hasPath(ConfigurationKeys.CONNECTOR)) {
      creator(conf.getString(ConfigurationKeys.CONNECTOR))
    } else {
      creator()
    }
  }

  def connector(config: Config): Connector = {
    val cnt = creator(config).map(_.create(config))
      .getOrElse(throw new ConnectorNotFoundException(config))
    if (config.hasPath(ConfigurationKeys.CREATE_TABLE) && config.getBoolean(ConfigurationKeys.CREATE_TABLE)) {
      cnt.createTable()
    } else if (!cnt.tableExists()) {
      log.warn(s"Darwin table does not exists and has not been created (${ConfigurationKeys.CREATE_TABLE} was false)")
      log.warn(cnt.tableCreationHint())
    }
    cnt
  }


}
