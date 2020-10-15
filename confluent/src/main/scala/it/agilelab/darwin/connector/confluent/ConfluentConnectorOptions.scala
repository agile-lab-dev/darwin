package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config

import scala.collection.JavaConverters._

case class ConfluentConnectorOptions(
  endpoints: List[String],
  subject: String,
  config: Map[String, _],
  maxCachedSchemas: Int
)

object ConfluentConnectorOptions {
  def fromConfig(config: Config): ConfluentConnectorOptions = {

    val endpoints        = config.getStringList("endpoints").asScala.toList
    val subject          = config.getString("subject")
    val maxCachedSchemas = config.getInt("max-cached-schemas")
    val other            = config.root().unwrapped().asScala.toMap

    ConfluentConnectorOptions(endpoints, subject, other, maxCachedSchemas)

  }
}
