import sbt.Keys.baseDirectory

/*
 * Main build definition.
 *
 * See project/Settings.scala for the settings definitions.
 * See project/Dependencies.scala for the dependencies definitions.
 * See project/Versions.scala for the versions definitions.
 */
dynverVTagPrefix in ThisBuild := false

lazy val root             = Project("darwin", file("."))
  .settings(Settings.commonSettings: _*)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(Settings.notPublishSettings)
  .aggregate(
    core,
    coreCommon,
    hbaseConnector,
    postgresConnector,
    mockConnector,
    mockApplication,
    restConnector,
    mongoConnector,
    confluentConnector
  )

lazy val core = Project("darwin-core", file("core"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)

lazy val coreCommon = Project("darwin-core-common", file("common"))
  .settings(Settings.commonSettings: _*)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)

lazy val hbaseConnector = Project("darwin-hbase-connector", file("hbase1"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.hbase_conn_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Compile / unmanagedSourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "main" / "scala")
  .settings(Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "test" / "scala")
  .settings(Test / unmanagedResourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "test" / "resources")
  .settings(Settings.hbaseTestSettings)

lazy val hbaseConnector2 = Project("darwin-hbase2-connector", file("hbase2"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.hbase2_conn_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Compile / unmanagedSourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "main" / "scala")
  .settings(Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "test" / "scala")
  .settings(Test / unmanagedResourceDirectories += baseDirectory.value / ".." / "hbase" / "src" / "test" / "resources")
  .settings(Settings.hbase2TestSettings)

lazy val postgresConnector = Project("darwin-postgres-connector", file("postgres"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.postgres_conn_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)

lazy val restConnector = Project("darwin-rest-connector", file("rest"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(
    libraryDependencies ++= Dependencies.core_deps ++ Dependencies.wireMock :+ Dependencies.scalatest :+
      Dependencies.httpClient
  )
  .settings(crossScalaVersions := Seq(Versions.scala, Versions.scala_211, Versions.scala_213))

lazy val confluentConnector = Project("darwin-confluent-connector", file("confluent"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(
    libraryDependencies ++= Dependencies.core_deps ++
      Dependencies.wireMock ++
      Dependencies.confluentSchemaRegistryDependencies :+ Dependencies.scalatest
  )
  .settings(crossScalaVersions := Versions.crossScalaVersions)

lazy val restServer = Project("darwin-rest-server", file("rest-server"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon, mockConnector)
  .settings(libraryDependencies ++= Dependencies.restServer)
  .settings(crossScalaVersions := Seq(Versions.scala, Versions.scala_211, Versions.scala_213))
  .dependsOn(core, hbaseConnector, postgresConnector, mockConnector)

lazy val mongoConnector = Project("darwin-mongo-connector", file("mongo"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.mongo_conn)
  .settings(crossScalaVersions := Seq(Versions.scala, Versions.scala_211, Versions.scala_213))

lazy val mockConnector = Project("darwin-mock-connector", file("mock-connector"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.mock_conn)
  .settings(crossScalaVersions := Versions.crossScalaVersions)

lazy val mockApplication = Project("darwin-mock-application", file("mock-application"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(core, mockConnector, postgresConnector, hbaseConnector)
  .settings(libraryDependencies ++= Dependencies.mock_app_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)

lazy val sparkApplication = Project("darwin-spark-application", file("spark-application"))
  .settings(Settings.commonSettings: _*)
  .dependsOn(core, hbaseConnector, postgresConnector)
  .settings(libraryDependencies ++= Dependencies.spark_app)
  .settings(crossScalaVersions := Seq(Versions.scala, Versions.scala_211))
  .settings(Settings.notPublishSettings)
