package it.agilelab.darwin.common

import java.util.ServiceLoader
import scala.collection.JavaConverters._

/**
  * Used to obtain the correct implementation of [[Connector]] found on the classpath using the [[ConnectorCreator]]
  */
object ConnectorFactory extends Logging {

  private val DEFAULT_PROVIDER: String = "TEST" //TODO set a default implementation

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

  def creator(): ConnectorCreator = creator(DEFAULT_PROVIDER)

  def creator(name: String): ConnectorCreator = {
    creators().find(_.getClass.getName == name) match {
      case Some(c) => c
      case _ => throw new ClassNotFoundException("Creator " + name + " not found")
    }
  }

}
