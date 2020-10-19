package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config
import it.agilelab.darwin.common.compat._

case class ConfluentConnectorOptions(
  endpoints: List[String],
  config: java.util.Map[String, AnyRef],
  maxCachedSchemas: Int
)

object ConfluentConnectorOptions {

  val ENDPOINTS_CONFIG_KEY   = "endpoints"
  val MAX_CACHED_SCHEMA_KEYS = "max-cached-schemas"

  def fromConfig(config: Config): ConfluentConnectorOptions = {

    if (!config.hasPath(ENDPOINTS_CONFIG_KEY)) {
      throw new IllegalArgumentException(
        s"Missing [${ENDPOINTS_CONFIG_KEY}] configuration key for ${classOf[ConfluentConnector].getName}"
      )
    }

    if (!config.hasPath(MAX_CACHED_SCHEMA_KEYS)) {
      throw new IllegalArgumentException(
        s"Missing [${MAX_CACHED_SCHEMA_KEYS}] configuration key for ${classOf[ConfluentConnector].getName}"
      )
    }

    val endpoints        = config.getStringList(ENDPOINTS_CONFIG_KEY).toScala().toList
    val maxCachedSchemas = config.getInt(MAX_CACHED_SCHEMA_KEYS)
    val other            = config.root()

    ConfluentConnectorOptions(endpoints, HoconToMap.convert(other), maxCachedSchemas)

  }
}
