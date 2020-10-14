package it.agilelab.darwin.connector.confluent

import com.typesafe.config.Config

import scala.collection.JavaConverters._

case class ConfluentConnectorOptions(endpoints: List[String], subject: String, config: Map[String, _])

object ConfluentConnectorOptions {
  def fromConfig(config: Config): ConfluentConnectorOptions = {

    val endpoints = config.getStringList("endpoints").asScala.toList
    val subject   = config.getString("subject")
    val other     = config.root().unwrapped().asScala.toMap

    ConfluentConnectorOptions(endpoints, subject, other)

  }
}
