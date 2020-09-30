package it.agilelab.darwin.common

import org.slf4j.{ Logger, LoggerFactory }

trait Logging {
  private lazy val _log = LoggerFactory.getLogger(getClass.getName)

  def log: Logger = _log
}
