package it.agilelab.darwin.manager

import com.typesafe.config.Config
import it.agilelab.darwin.common.Logging
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException
import it.agilelab.darwin.manager.util.ConfigUtil

/**
  * Factory used to obtain the desired implementation of AvroSchemaManager.
  * First of all the initialize method should be called passing the configuration (it will return an instance of
  * AvroSchemaManager. Then, the same instance can be retrieved using the getInstance method without passing the
  * configuration anymore.
  */
object AvroSchemaManagerFactory extends Logging {

  private var _instance: AvroSchemaManager = _

  /**
    * Returns an instance of AvroSchemaManager that can be used to register and retrieve schemas.
    *
    * @param config the Config that is passed to the connector
    * @return an instance of AvroSchemaManager
    */
  @throws[ConnectorNotFoundException]
  def initialize(config: Config): AvroSchemaManager = {
    synchronized {
      if (_instance == null) {
        log.debug("creating instance of AvroSchemaManager")
        _instance = config.getString(ConfigUtil.MANAGER_TYPE) match {
          case ConfigUtil.CACHED_EAGER => CachedEagerAvroSchemaManager(config)
          case ConfigUtil.CACHED_LAZY => CachedLazyAvroSchemaManager(config)
          case ConfigUtil.LAZY => LazyAvroSchemaManager(config)
          case _ => throw new IllegalArgumentException(s"No valid manager can be created for" +
            s" ${ConfigUtil.MANAGER_TYPE} key ${config.getString(ConfigUtil.MANAGER_TYPE)}")
        }
        log.debug("AvroSchemaManager instance created")
      }
      _instance
    }
  }

  /**
    * Returns the initialized instance of AvroSchemaManager that can be used to register and retrieve schemas.
    * The instance must be created once using the initialize method passing a configuration before calling this method.
    *
    * @return the initialized instance of AvroSchemaManager
    */
  def getInstance: AvroSchemaManager = {
    synchronized {
      if (_instance == null) {
        throw new IllegalArgumentException("Instance must be loaded with configuration")
      }
      _instance
    }
  }

}
