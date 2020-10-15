package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config

import scala.collection.JavaConverters._

case class ConfluentConnectorOptions(
  endpoints: List[String],
  config: Map[String, _],
  maxCachedSchemas: Int
)

object ConfluentConnectorOptions {
  def fromConfig(config: Config): ConfluentConnectorOptions = {

    val endpoints        = config.getStringList("endpoints").asScala.toList
    val maxCachedSchemas = config.getInt("max-cached-schemas")
    val other            = config.root().unwrapped().asScala.toMap

    ConfluentConnectorOptions(endpoints, other, maxCachedSchemas)

  }
}
