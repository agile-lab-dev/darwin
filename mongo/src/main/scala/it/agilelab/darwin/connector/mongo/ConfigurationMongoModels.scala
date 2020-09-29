package it.agilelab.darwin.connector.mongo

import scala.concurrent.duration.Duration

object ConfigurationMongoModels {

  sealed trait BaseMongoConfig {
    def database: String
    def collection: String
    def timeout: Duration
  }

  case class MongoConfig(
    database: String,
    collection: String,
    timeout: Duration
  ) extends BaseMongoConfig

  case class MongoConnectorConfig(
    username: String,
    password: String,
    database: String,
    collection: String,
    hosts: Seq[String],
    timeout: Duration
  ) extends BaseMongoConfig

  val DEFAULT_DURATION = 5000

}
