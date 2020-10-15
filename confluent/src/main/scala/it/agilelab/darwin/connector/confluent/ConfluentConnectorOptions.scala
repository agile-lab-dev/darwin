package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config

import it.agilelab.darwin.common.compat._

case class ConfluentConnectorOptions(
  endpoints: List[String],
  config: Map[String, _],
  maxCachedSchemas: Int
)

object ConfluentConnectorOptions {
  def fromConfig(config: Config): ConfluentConnectorOptions = {

    val endpoints        = config.getStringList("endpoints").toScala().toList
    val maxCachedSchemas = config.getInt("max-cached-schemas")
    val other            = config.root().unwrapped().toScala().toMap

    ConfluentConnectorOptions(endpoints, other, maxCachedSchemas)

  }
}
