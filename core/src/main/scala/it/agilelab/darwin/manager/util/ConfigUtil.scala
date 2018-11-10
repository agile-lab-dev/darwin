package it.agilelab.darwin.manager.util

import com.typesafe.config.{Config, ConfigRenderOptions}

object ConfigUtil {
  def printConfig(conf: Config): String = {
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
  }
  val MANAGER_TYPE: String = "type"
  val CACHED_EAGER: String = "cached_eager"
  val CACHED_LAZY: String = "cached_lazy"
  val LAZY: String = "lazy"
}
