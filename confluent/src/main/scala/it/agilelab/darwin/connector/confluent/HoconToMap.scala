package it.agilelab.darwin.connector.confluent

import com.typesafe.config.{ ConfigObject, ConfigValue }
import it.agilelab.darwin.common.compat.{ JMapConverter, SetConverter }

import scala.collection.mutable

private[confluent] object HoconToMap {

  private def walk(root: ConfigValue): Map[String, AnyRef] = {
    val result = mutable.HashMap.empty[String, AnyRef]

    def doWalk(path: String, r: ConfigValue): Unit = {

      r match {
        case o: ConfigObject =>
          o.keySet().toScala().foreach { key =>
            val nextPath = if (path.isEmpty) key else path + "." + key
            doWalk(nextPath, o.get(key))
          }
        case _               =>
          result += path -> r.unwrapped()
      }
    }

    doWalk("", root)

    result.toMap
  }

  def convert(configValue: ConfigValue): java.util.Map[String, AnyRef] = {
    walk(configValue).toJava()
  }
}
