import sbt.Def
import sbt.Keys._
import sbt._
import com.typesafe.sbt.pgp.PgpKeys.pgpPassphrase
/**
  * @author andreaL
  */
object Settings {

  val SCALA_210 = Some((2L, 10L))
  val SCALA_211 = Some((2L, 11L))
  val SCALA_212 = Some((2L, 12L))

  def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
    Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Xlint",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    ) ++ {
      CrossVersion.partialVersion(scalaVersion) match {
        case SCALA_210 =>
          Nil
        case SCALA_211 =>
          Seq("-Xfatal-warnings", "-Ywarn-unused-import", "-Ywarn-infer-any")
        case SCALA_212 =>
          Seq("-Xfatal-warnings", "-Ywarn-unused-import", "-Ywarn-infer-any")
        case version: Option[(Long, Long)] =>
          throw new Exception(s"Unknown scala version: $version")
      }
    }
  }

  def scalaDocOptionsVersion(scalaVersion: String): Seq[String] = {
    scalacOptionsVersion(scalaVersion) ++ {
      CrossVersion.partialVersion(scalaVersion) match {
        case SCALA_210 | SCALA_211 => Nil
        case SCALA_212 => Seq("-no-java-comments")
        case version: Option[(Long, Long)] => throw new Exception(s"Unknown scala version: $version")
      }
    }
  }


  lazy val projectSettings = Seq(
    organization := "it.agilelab",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage := Some(url("https://github.com/agile-lab-dev/darwin")),
    description := "Avro Schema Evolution made easy",
    scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
    scalacOptions.in(Compile, doc) ++= scalaDocOptionsVersion(scalaVersion.value),
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


  lazy val hbaseTestSettings: SettingsDefinition = {
    //enable resolution of transitive dependencies of jars containing tests
    //needed to run tests over hbase minicluster
    transitiveClassifiers in Test := Seq(Artifact.TestsClassifier, Artifact.SourceClassifier)
    libraryDependencies  ++= Dependencies.hbaseTestDependencies
  }

  lazy val notPublishSettings = Seq(skip in publish := true)

  lazy val myCredentials = Credentials(
    "Bintray API Realm",
    "api.bintray.com",
    System.getenv().get("BINTRAY_USERNAME"),
    System.getenv().get("BINTRAY_API_KEY")
  )

  lazy val pgpPass: Option[Array[Char]] = Option(System.getenv().get("PGP_PASS")).map(_.toArray)

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
      </developers>)
}
