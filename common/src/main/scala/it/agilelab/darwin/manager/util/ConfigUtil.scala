package it.agilelab.darwin.manager.util

import com.typesafe.config.{Config, ConfigRenderOptions}

object ConfigUtil {
  def printConfig(conf: Config): String = {
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
  }

  def printSmallConfig(conf: Config): String = {
    conf.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
  }

}
