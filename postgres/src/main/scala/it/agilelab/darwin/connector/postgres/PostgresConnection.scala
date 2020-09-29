package it.agilelab.darwin.connector.postgres

import java.sql.{ Connection, DriverManager }

import com.typesafe.config.Config

trait PostgresConnection {

  private var connectionUrl: String = ""
  private val driverName: String    = "org.postgresql.Driver"

  protected def setConnectionConfig(config: Config) = {
    val db       = config.getString(ConfigurationKeys.DATABASE)
    val host     = config.getString(ConfigurationKeys.HOST)
    val user     = config.getString(ConfigurationKeys.USER)
    val password = config.getString(ConfigurationKeys.PASSWORD)
    connectionUrl = s"jdbc:postgresql://$host/$db?user=$user&password=$password"
  }

  protected def getConnection: Connection = {
    Class.forName(driverName)
    val connection: Connection = DriverManager.getConnection(connectionUrl)
    connection
  }
}
