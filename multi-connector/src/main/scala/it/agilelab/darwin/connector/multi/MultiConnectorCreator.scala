package it.agilelab.darwin.connector.multi

import com.typesafe.config.Config
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{ Connector, ConnectorCreator, ConnectorFactory }
import it.agilelab.darwin.manager.exception.DarwinException

object MultiConnectorCreator {
  val REGISTRATOR                      = "registrar"
  val CONFLUENT_SINGLE_OBJECT_ENCODING = "confluent-single-object-encoding"
  val STANDARD_SINGLE_OBJECT_ENCODING  = "standard-single-object-encoding"
}
class MultiConnectorCreator extends ConnectorCreator {

  /**
    * @return the name of the Connector
    */
  override def name(): String = "multi"

  private def mergeConf(conf: Config, path: String): Config = {

    conf
      .getConfig(path)
      .entrySet()
      .toScala()
      .map(_.getKey)
      .foldLeft(conf)((z, x) => z.withValue(x, conf.getValue(path + "." + x)))
  }

  override def create(config: Config): Connector = {
    val registrarName =
      config.getString(MultiConnectorCreator.REGISTRATOR)

    val confluentConnectorType =
      if (config.hasPath(MultiConnectorCreator.CONFLUENT_SINGLE_OBJECT_ENCODING)) {
        Some(config.getString(MultiConnectorCreator.CONFLUENT_SINGLE_OBJECT_ENCODING))
      } else {
        None
      }

    val standardConnectorTypes = config
      .getStringList(MultiConnectorCreator.STANDARD_SINGLE_OBJECT_ENCODING)
      .toScala()

    val registrar = createAndMergeConfigs(config, registrarName)

    val confluentConnector =
      confluentConnectorType.map { cName =>
        createIfNotRegistrar(registrarName, registrar, cName, config)
      }

    val singleObjectConnectors = standardConnectorTypes.map { cName =>
      createIfNotRegistrar(registrarName, registrar, cName, config)
    }.toList

    new MultiConnector(
      registrar,
      confluentConnector,
      singleObjectConnectors
    )
  }

  private def createAndMergeConfigs(config: Config, registrarName: String) = {
    ConnectorFactory
      .creator(registrarName)
      .map(creator => creator.create(mergeConf(config, registrarName)))
      .getOrElse(throw new DarwinException("No connector creator for name " + registrarName))
  }

  private def createIfNotRegistrar(registrarName: String, registrar: Connector, cName: String, config: Config) = {
    if (cName == registrarName) {
      registrar
    } else {
      ConnectorFactory
        .creator(cName)
        .map(creator => creator.create(mergeConf(config, cName)))
        .getOrElse(throw new DarwinException("No connector creator for name " + cName))
    }
  }
}
