package it.agilelab.darwin.manager

import com.typesafe.config.Config
import it.agilelab.darwin.common.{ ConnectorFactory, DarwinConcurrentHashMap, Logging }
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException
import it.agilelab.darwin.manager.util.{ ConfigUtil, ConfigurationKeys }

/**
  * Factory used to obtain the desired implementation of AvroSchemaManager.
  * First of all the initialize method should be called passing the configuration (it will return an instance of
  * AvroSchemaManager. Then, the same instance can be retrieved using the getInstance method without passing the
  * configuration anymore.
  */
object AvroSchemaManagerFactory extends Logging {

  private val _instancePool: DarwinConcurrentHashMap[String, AvroSchemaManager] =
    DarwinConcurrentHashMap.empty[String, AvroSchemaManager]

  private def configKey(c: Config): String = {
    ConfigUtil.printConfig(c)
  }

  /**
    * Returns an instance of AvroSchemaManager that can be used to register and retrieve schemas.
    *
    * @param config the Config that is passed to the connector
    * @return an instance of AvroSchemaManager
    */
  @throws[ConnectorNotFoundException]
  def initialize(config: Config): AvroSchemaManager = {
    val key = configKey(config)
    lazy val mappingFunc = {
      log.debug("creating instance of AvroSchemaManager")
      val endianness = ConfigUtil.stringToEndianness(config.getString(ConfigurationKeys.ENDIANNESS))
      val result     = config.getString(ConfigurationKeys.MANAGER_TYPE) match {
        case ConfigurationKeys.CACHED_EAGER =>
          new CachedEagerAvroSchemaManager(ConnectorFactory.connector(config), endianness)
        case ConfigurationKeys.CACHED_LAZY  =>
          new CachedLazyAvroSchemaManager(ConnectorFactory.connector(config), endianness)
        case ConfigurationKeys.LAZY         =>
          new LazyAvroSchemaManager(ConnectorFactory.connector(config), endianness)
        case _                              =>
          throw new IllegalArgumentException(
            s"No valid manager can be created for" +
              s" ${ConfigurationKeys.MANAGER_TYPE} key ${config.getString(ConfigurationKeys.MANAGER_TYPE)}"
          )
      }
      log.debug("AvroSchemaManager instance created")
      result
    }
    _instancePool.getOrElseUpdate(key, mappingFunc)
  }

  /**
    * Returns the initialized instance of AvroSchemaManager that can be used to register and retrieve schemas.
    * The instance must be created once using the initialize method passing a configuration before calling this method.
    *
    * @return the initialized instance of AvroSchemaManager
    */
  def getInstance(config: Config): AvroSchemaManager = {
    _instancePool.getOrElse(
      configKey(config),
      throw new IllegalArgumentException(
        s"No valid manager can be found for" +
          s" ${ConfigurationKeys.MANAGER_TYPE} key ${config.getString(ConfigurationKeys.MANAGER_TYPE)}"
      )
    )
  }

}
