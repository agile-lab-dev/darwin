package it.agilelab.darwin.connector.mock

object ConfigurationKeys {
  val FILES      = "files"
  val RESOURCES  = "resources"
  val MODE       = "mode"
  val STRICT     = "strict"
  val PERMISSIVE = "permissive"

  sealed trait Mode

  object Mode {
    def parse(string: String): Mode = {
      string.toLowerCase match {
        case STRICT        => Strict
        case PERMISSIVE    => Permissive
        case other: String => throw new IllegalArgumentException(s"Unknown mode: $other")
      }
    }
  }

  case object Strict extends Mode

  case object Permissive extends Mode

}
