package it.agilelab.darwin.manager

import com.typesafe.config.Config
import it.agilelab.darwin.common.Logging
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException

object AvroSchemaManagerFactory extends Logging {

  private var _instance: AvroSchemaManager = _

  /**
    * Returns an instance of AvroSchemaManager that can be used to register schemas.
    *
    * @param config the Config that is passed to the connector
    * @return an instance of AvroSchemaManager
    */
  @throws[ConnectorNotFoundException]
  def getInstance(config: Config): AvroSchemaManager = {
    synchronized {
      if (_instance == null) {
        log.debug("creating instance of AvroSchemaManager")
        _instance = EagerAvroSchemaManager(config)
        log.debug("AvroSchemaManager instance created")
      }
    }
    _instance
  }

}
