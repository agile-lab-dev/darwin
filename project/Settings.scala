import sbt.Def
import sbt.Keys._
import sbt._

/**
  * @author andreaL
  */
object Settings {

  lazy val projectSettings = Seq(
    organization := "it.agilelab",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/agile-lab-dev/darwin"))
  )

  val clouderaHadoopReleaseRepo = "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

  lazy val customResolvers = Seq(
    clouderaHadoopReleaseRepo
  )

  lazy val buildSettings = Seq(
    resolvers ++= customResolvers,
    scalaVersion := Versions.scala
  )

  lazy val commonSettings: Seq[Def.SettingsDefinition] = projectSettings ++ buildSettings ++ publishSettings

  lazy val notPublishSettings = Seq(skip in publish := true)
  
  lazy val myCredentials = Credentials(
    "Bintray API Realm",
    "api.bintray.com",
    System.getenv().get("BINTRAY_USERNAME"),
    System.getenv().get("BINTRAY_API_KEY")
  )

  lazy val publishSettings = Seq(
    publishTo := Some("bintray" at "https://api.bintray.com/maven/agile-lab-dev/Darwin/darwin/;publish=1"),
    credentials += myCredentials,
    publishMavenStyle := true,
    updateOptions := updateOptions.value.withGigahorse(false),
    pomExtra := <scm>
    <connection>
      scm:git:git://github.com/agile-lab-dev/darwin.git
    </connection>
    <url>
      https://github.com/agile-lab-dev/darwin
    </url>
  </scm>
  <developers>
    <developer>
      <id>amurgia</id>
      <name>Antonio Murgia</name>
      <email>antonio.murgia@agilelab.it</email>
    </developer>
  </developers>)

}
