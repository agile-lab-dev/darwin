package it.agilelab.darwin.common

import java.util.ServiceLoader

import com.typesafe.config.Config
import it.agilelab.darwin.manager.util.ConfigurationKeys

import scala.collection.JavaConverters._

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
    val creators = ServiceLoader.load(classOf[ConnectorCreator]).asScala.toSeq
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


}
