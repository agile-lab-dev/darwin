package it.agilelab.darwin.manager.util

import java.nio.ByteOrder

import com.typesafe.config.{Config, ConfigRenderOptions}

object ConfigUtil {
  def printConfig(conf: Config): String = {
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
  }

  def printSmallConfig(conf: Config): String = {
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
  }

  def stringToEndianness(string: String): ByteOrder = {
    string.toUpperCase match {
      case "BIG_ENDIAN" => ByteOrder.BIG_ENDIAN
      case "LITTLE_ENDIAN" => ByteOrder.LITTLE_ENDIAN
      case _ => throw new IllegalArgumentException(s"Unknown endianness: $string")
    }
  }

  def getOptInt(conf: Config, path: String): Option[Int] =
    if (conf.hasPath(path)) Some(conf.getInt(path)) else None

  def getOptBoolean(conf: Config, path: String): Option[Boolean] =
    if (conf.hasPath(path)) Some(conf.getBoolean(path)) else None

  def getOptConfig(conf: Config, path: String): Option[Config] =
    if (conf.hasPath(path)) Some(conf.getConfig(path)) else None

  def getOptString(conf: Config, path: String): Option[String] =
    if (conf.hasPath(path)) Some(conf.getString(path)) else None

  def getOptLong(conf: Config, path: String): Option[Long] =
    if (conf.hasPath(path)) Some(conf.getLong(path)) else None

}
