package it.agilelab.darwin.connector.hbase

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorCreator, Logging}
import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.apache.hadoop.hbase.security.User
import org.apache.hadoop.security.UserGroupInformation

import scala.collection.JavaConverters._

class HBaseConnectorCreator extends ConnectorCreator with Logging {

  val DEFAULT_NAMESPACE: String = "AVRO"
  val DEFAULT_TABLENAME: String = "SCHEMA_REPOSITORY"

  override def create(config: Config): Connector = {
    log.debug("creating the HBase connector")

    val TABLE_NAME_STRING: String = if (config.hasPath(ConfigurationKeys.TABLE)) {
      config.getString(ConfigurationKeys.TABLE)
    } else {
      DEFAULT_TABLENAME
    }

    val NAMESPACE_STRING: String = if (config.hasPath(ConfigurationKeys.NAMESPACE)) {
      config.getString(ConfigurationKeys.NAMESPACE)
    } else {
      DEFAULT_NAMESPACE
    }

    val connector: Connector = HBaseConnector(getConnection(config), NAMESPACE_STRING, TABLE_NAME_STRING)
    log.debug("HBase connector created")
    connector
  }

  private def addResourceMessage(s: String) = {
    val ADDING_RESOURCE = "Adding resource: "
    ADDING_RESOURCE + s
  }

  private def getConnection(config: Config) = {
    log.debug("Creating default HBaseConfiguration")
    val configuration: Configuration = HBaseConfiguration.create()
    log.debug("Created default HBaseConfiguration")

    if (config.hasPath(ConfigurationKeys.CORE_SITE) && config.hasPath(ConfigurationKeys.HBASE_SITE)) {
      log.debug(addResourceMessage(config.getString(ConfigurationKeys.CORE_SITE)))
      configuration.addResource(new Path(config.getString(ConfigurationKeys.CORE_SITE)))
      log.debug(addResourceMessage(config.getString(ConfigurationKeys.HBASE_SITE)))
      configuration.addResource(new Path(config.getString(ConfigurationKeys.HBASE_SITE)))
    }

    val connection: Connection = if (config.getBoolean(ConfigurationKeys.IS_SECURE)) {
      log.debug(s"Calling UserGroupInformation.setConfiguration()")
      UserGroupInformation.setConfiguration(configuration)

      log.debug(s"Calling UserGroupInformation.loginUserFromKeytab(${config.getString(ConfigurationKeys.PRINCIPAL)}, " +
        s"${config.getString(ConfigurationKeys.KEYTAB_PATH)})")
      val ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(
        config.getString(ConfigurationKeys.PRINCIPAL), config.getString(ConfigurationKeys.KEYTAB_PATH)
      )
      UserGroupInformation.setLoginUser(ugi)
      val user = User.create(ugi)
      log.trace(s"initialization of HBase connection with configuration:\n " +
        s"${configuration.iterator().asScala.map { entry => entry.getKey -> entry.getValue }.mkString("\n")}")
      ConnectionFactory.createConnection(configuration, user)
    } else {
      log.trace(s"initialization of HBase connection with configuration:\n " +
        s"${configuration.iterator().asScala.map { entry => entry.getKey -> entry.getValue }.mkString("\n")}")
      ConnectionFactory.createConnection(configuration)
    }

    log.debug("HBase connection initialized")
    sys.addShutdownHook {
      //  log.info(s"closing HBase connection pool")
      IOUtils.closeQuietly(connection)
    }
    connection
  }

  /**
    * @return the name of the Connector
    */
  override def name(): String = "hbase"
}
