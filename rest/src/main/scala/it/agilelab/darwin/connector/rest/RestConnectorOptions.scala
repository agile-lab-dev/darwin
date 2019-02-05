package it.agilelab.darwin.connector.rest

import java.net.URI

import com.typesafe.config.Config

case class RestConnectorOptions(protocol: String, host: String, port: Int, basePath: String) {
  def endpoint(path: String): String =
    URI.create(s"$protocol://$host:$port").resolve(basePath).resolve(path).toString
}

object RestConnectorOptions {

  private val PROTOCOL = "protocol"
  private val HOST = "host"
  private val PORT = "port"
  private val BASE_PATH = "basePath"


  def fromConfig(config: Config): RestConnectorOptions =
    RestConnectorOptions(config.getString(PROTOCOL),
      config.getString(HOST),
      config.getInt(PORT),
      config.getString(BASE_PATH))
}
