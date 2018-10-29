import sbt.Def
import sbt.Keys._
import sbt._

/**
  * @author andreaL
  */
object Settings {

  lazy val projectSettings = Seq(
    organization := "it.agilelab"
  )

  val clouderaHadoopReleaseRepo = "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

  lazy val customResolvers = Seq(
    clouderaHadoopReleaseRepo
  )

  lazy val buildSettings = Seq(
    resolvers ++= customResolvers,
    scalaVersion := Versions.scala
  )

  lazy val commonSettings: Seq[Def.SettingsDefinition] = projectSettings ++ buildSettings
}
