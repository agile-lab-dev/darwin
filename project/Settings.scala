import sbt.Def
import sbt.Keys._
import sbt._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import com.typesafe.sbt.SbtPgp.autoImport._

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
    scalacOptions.in(Compile, doc) ++= scalaDocOptionsVersion(scalaVersion.value),
    useCoursier := true
  )

  val clouderaHadoopReleaseRepo = "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

  lazy val customResolvers = Seq(
    clouderaHadoopReleaseRepo
  )

  lazy val buildSettings = Seq(
    resolvers ++= customResolvers,
    scalaVersion := Versions.scala
  )

//  lazy val coursierSettings = Seq(classpathTypes += "test-jar")

  lazy val commonSettings: Seq[Def.SettingsDefinition] = projectSettings ++ buildSettings ++ publishSettings ++
    scalastyleSettings // ++ coursierSettings

  lazy val hbaseTestSettings: SettingsDefinition = {
    //enable resolution of transitive dependencies of jars containing tests
    //needed to run tests over hbase minicluster
    transitiveClassifiers in Test := Seq(Artifact.TestsClassifier, Artifact.SourceClassifier)
    libraryDependencies ++= Dependencies.hbaseTestDependencies
  }

  lazy val hbase2TestSettings: SettingsDefinition = {
    //enable resolution of transitive dependencies of jars containing tests
    //needed to run tests over hbase minicluster
    transitiveClassifiers in Test := Seq(Artifact.TestsClassifier, Artifact.SourceClassifier)
    libraryDependencies ++= Dependencies.hbase2TestDependencies
  }

  lazy val notPublishSettings = Seq(skip in publish := true)

  lazy val myCredentials = Credentials(
    "Bintray API Realm",
    "api.bintray.com",
    System.getenv().get("BINTRAY_USERNAME"),
    System.getenv().get("BINTRAY_API_KEY")
  )

  lazy val ciPublishSettings = {
    if (System.getenv().containsKey("TRAVIS")) {
      Seq(pgpSecretRing := file("./secring.asc"), pgpPublicRing := file("./pubring.asc"))
    } else {
      Seq.empty
    }
  }

  lazy val pgpPass: Option[Array[Char]] = Option(System.getenv().get("PGP_PASS")).map(_.toArray)

  lazy val scalastyleSettings = Seq(scalastyleFailOnWarning := true)

//  lazy val testSettings = Seq(parallelExecution in Test := false)

  lazy val publishSettings = Seq(
    publishTo := Some("bintray" at "https://api.bintray.com/maven/agile-lab-dev/Darwin/darwin/;publish=1"),
    credentials += myCredentials,
    publishMavenStyle := true,
    updateOptions := updateOptions.value.withGigahorse(false),
    useGpg := false,
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
        <developer>
          <id>lpirazzini</id>
          <name>Lorenzo Pirazzini</name>
          <email>lorenzo.pirazzini@agilelab.it</email>
        </developer>
        <developer>
          <id>rcoluccio</id>
          <name>Roberto Coluccio</name>
          <email>roberto.coluccio@agilelab.it</email>
        </developer>
        <developer>
          <id>alatella</id>
          <name>Andrea Latella</name>
          <email>andrea.latella@agilelab.it</email>
        </developer>
        <developer>
          <id>cventrella</id>
          <name>Carlo Ventrella</name>
          <email>carlo.ventrella@agilelab.it</email>
        </developer>
        <developer>
          <id>dicardi</id>
          <name>Davide Icardi</name>
          <email>davide.icardi@agilelab.it</email>
        </developer>
        <developer>
          <id>nbidotti</id>
          <name>Nicolò Bidotti</name>
          <email>nicolo.bidotti@agilelab.it</email>
        </developer>
        <developer>
          <id>andrea-rockt</id>
          <name>Andrea Fonti</name>
          <email>andrea.fonti@agilelab.it</email>
        </developer>
      </developers>
  ) ++ ciPublishSettings
}
