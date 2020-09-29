package it.agilelab.darwin.connector.mongo

import com.mongodb.Block
import com.typesafe.config.Config
import it.agilelab.darwin.common.{ Connector, ConnectorCreator }
import it.agilelab.darwin.connector.mongo.ConfigurationMongoModels.MongoConnectorConfig
import org.mongodb.scala.connection.ClusterSettings
import org.mongodb.scala.{ MongoClient, MongoClientSettings, MongoCredential, ServerAddress }
import it.agilelab.darwin.common.compat._
import scala.concurrent.duration.Duration

class MongoConnectorCreator extends ConnectorCreator {

  override def create(config: Config): Connector = {

    val mongoConfig: MongoConnectorConfig = createConfig(config)
    new MongoConnector(createConnection(mongoConfig), mongoConfig)
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "mongo"

  /**
    * return the MongoClient
    * @param mongoConf : config to create a connection to MongoDB
    * @return MongoClient
    */
  private def createConnection(mongoConf: MongoConnectorConfig): MongoClient = {

    val credential: MongoCredential =
      MongoCredential.createCredential(mongoConf.username, mongoConf.database, mongoConf.password.toCharArray)

    val hosts: Seq[ServerAddress] = mongoConf.hosts.map(host => new ServerAddress(host))

    val settings: MongoClientSettings = MongoClientSettings
      .builder()
      .credential(credential)
      .applyToClusterSettings(new Block[ClusterSettings.Builder] {
        override def apply(builder: ClusterSettings.Builder): Unit =
          builder.hosts(java.util.Arrays.asList(hosts: _*))
      })
      .build()

    MongoClient(settings)
  }

  /**
    * create MongoConnectorConfig started from a configuration file
    * @param config: configurations parsed from the file
    * @return MongoConnectorConfig
    */
  def createConfig(config: Config): MongoConnectorConfig = {
    require(config.hasPath(ConfigurationKeys.USERNAME))
    require(config.hasPath(ConfigurationKeys.PASSWORD))
    require(config.hasPath(ConfigurationKeys.HOST))
    require(config.hasPath(ConfigurationKeys.DATABASE))
    require(config.hasPath(ConfigurationKeys.COLLECTION))

    MongoConnectorConfig(
      config.getString(ConfigurationKeys.USERNAME),
      config.getString(ConfigurationKeys.PASSWORD),
      config.getString(ConfigurationKeys.DATABASE),
      config.getString(ConfigurationKeys.COLLECTION),
      config.getStringList(ConfigurationKeys.HOST).toScala().toSeq,
      if (config.hasPath(ConfigurationKeys.TIMEOUT)) {
        Duration.create(config.getInt(ConfigurationKeys.TIMEOUT), "millis")
      } else {
        Duration.create(ConfigurationMongoModels.DEFAULT_DURATION, "millis")
      }
    )
  }

}
