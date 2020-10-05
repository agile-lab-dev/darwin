package it.agilelab.darwin.manager.util

import java.nio.ByteOrder

import com.typesafe.config.{ Config, ConfigRenderOptions }

object ConfigUtil {
  def printConfig(conf: Config): String =
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))

  def printSmallConfig(conf: Config): String =
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))

  def stringToEndianness(string: String): ByteOrder =
    string.toUpperCase match {
      case "BIG_ENDIAN"    => ByteOrder.BIG_ENDIAN
      case "LITTLE_ENDIAN" => ByteOrder.LITTLE_ENDIAN
      case _               => throw new IllegalArgumentException(s"Unknown endianness: $string")
    }

}
