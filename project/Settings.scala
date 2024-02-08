import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbt.Keys._
import sbt.{ Def, _ }

/**
  * @author andreaL
  */
object Settings {

  val SCALA_210 = Some((2L, 10L))
  val SCALA_211 = Some((2L, 11L))
  val SCALA_212 = Some((2L, 12L))
  val SCALA_213 = Some((2L, 13L))

  def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
    Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-encoding",
      "UTF-8"
    ) ++ {
      CrossVersion.partialVersion(scalaVersion) match {
        case SCALA_210                     =>
          Seq("-target:jvm-1.7", "-Ywarn-inaccessible")
        case SCALA_211                     =>
          Seq("-Xfatal-warnings", "-Ywarn-inaccessible", "-Ywarn-unused-import", "-Ywarn-infer-any", "-target:jvm-1.7")
        case SCALA_212                     =>
          Seq("-Xfatal-warnings", "-Ywarn-inaccessible", "-Ywarn-unused-import", "-Ywarn-infer-any", "-target:jvm-1.8")
        case SCALA_213                     =>
          Seq("-Xfatal-warnings", "-Xlint:inaccessible", "-Ywarn-unused:imports", "-Xlint:infer-any", "-target:jvm-1.8")
        case version: Option[(Long, Long)] =>
          throw new Exception(s"Unknown scala version: $version")
      }
    }
  }

  def scalaDocOptionsVersion(scalaVersion: String): Seq[String] = {
    CrossVersion.partialVersion(scalaVersion) match {
      case SCALA_210 | SCALA_211         => scalacOptionsVersion(scalaVersion)
      case SCALA_212                     => scalacOptionsVersion(scalaVersion) ++ Seq("-no-java-comments")
      case SCALA_213                     => scalacOptionsVersion(scalaVersion) ++ Seq("-no-java-comments")
      case version: Option[(Long, Long)] => throw new Exception(s"Unknown scala version: $version")
    }
  }

  def javacOptionsVersion(scalaVersion: String): Seq[String] = {
    CrossVersion.partialVersion(scalaVersion) match {
      case SCALA_210                     =>
        Seq("-source", "1.7", "-target", "1.7")
      case SCALA_211                     =>
        Seq("-source", "1.7", "-target", "1.7")
      case SCALA_212                     =>
        Seq("-source", "1.8", "-target", "1.8")
      case SCALA_213                     =>
        Seq("-source", "1.8", "-target", "1.8")
      case version: Option[(Long, Long)] =>
        throw new Exception(s"Unknown scala version: $version")
    }
  }

  lazy val projectSettings = Seq(
    organization := "it.agilelab",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/agile-lab-dev/darwin")),
    description := "Avro Schema Evolution made easy",
    javacOptions ++= javacOptionsVersion(scalaVersion.value),
    scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
    Compile / doc / scalacOptions ++= scalaDocOptionsVersion(scalaVersion.value),
    versionScheme := Some("early-semver"),
    useCoursier := false,
    developers := List(
      Developer("amurgia", "Antonio Murgia", "antonio.murgia@agilelab.it", url("https://github.com/tmnd1991")),
      Developer("lpirazzini", "Lorenzo Pirazzini", "lorenzo.pirazzini@agilelab.it", url("https://github.com/SpyQuel")),
      Developer("rcoluccio", "Roberto Coluccio", "roberto.coluccio@agilelab.it", url("https://github.com/erond")),
      Developer("alatella", "Andrea Latella", "andrea.latella@agilelab.it", url("https://github.com/andr3a87")),
      Developer("cventrella", "Carlo Ventrella", "carlo.ventrella@agilelab.it", url("https://www.agilelab.it")),
      Developer("dicardi", "Davide Icardi", "davide.icardi@agilelab.it", url("https://github.com/davideicardi")),
      Developer("nbidotti", "Nicolò Bidotti", "nicolo.bidotti@agilelab.it", url("https://github.com/nicolobidotti")),
      Developer("andrea-rockt", "Andrea Fonti", "andrea.fonti@agilelab.it", url("https://github.com/andrea-rockt"))
    )
  )

  val clouderaHadoopReleaseRepo = "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
  val confluent                 = "confluent" at "https://packages.confluent.io/maven/"

  lazy val customResolvers = Seq(
    clouderaHadoopReleaseRepo,
    confluent
  )

  lazy val buildSettings: Seq[SettingsDefinition] = {
    //this is an hack to resolve correctly rs-api
    // [warn] [FAILED     ] javax.ws.rs#javax.ws.rs-api;2.1!javax.ws.rs-api.${packaging.type}:  (0ms)
    // https://github.com/sbt/sbt/issues/3618
    sys.props += "packaging.type" -> "jar"
    Seq(
      resolvers ++= customResolvers,
      scalaVersion := Versions.scala
    )
  }

  lazy val commonSettings = projectSettings ++ buildSettings ++ scalastyleSettings

  lazy val hbaseTestSettings: SettingsDefinition = {
    //enable resolution of transitive dependencies of jars containing tests
    //needed to run tests over hbase minicluster
    Test / transitiveClassifiers := Seq(Artifact.TestsClassifier, Artifact.SourceClassifier)
    libraryDependencies ++= Dependencies.hbaseTestDependencies
  }

  lazy val hbase2TestSettings: SettingsDefinition = {
    //enable resolution of transitive dependencies of jars containing tests
    //needed to run tests over hbase minicluster
    Test / transitiveClassifiers := Seq(Artifact.TestsClassifier, Artifact.SourceClassifier)
    libraryDependencies ++= Dependencies.hbase2TestDependencies
  }

  lazy val notPublishSettings = Seq(publish / skip := true)

  lazy val scalastyleSettings = Seq(scalastyleFailOnWarning := true)
}
