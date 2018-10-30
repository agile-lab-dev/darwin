/*
 * Main build definition.
 *
 * See project/Settings.scala for the settings definitions.
 * See project/Dependencies.scala for the dependencies definitions.
 * See project/Versions.scala for the versions definitions.
 */


lazy val root = Project("darwin", file("."))
  .settings(Settings.commonSettings:_*)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)
  .aggregate(core, coreCommon, hbaseConnector, postgresConnector)

lazy val core = Project("darwin-core", file("core"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .enablePlugins(JavaAppPackaging)

lazy val coreCommon = Project("darwin-core-common", file("common"))
  .settings(Settings.commonSettings:_*)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .enablePlugins(JavaAppPackaging)

lazy val hbaseConnector = Project("darwin-hbase-connector", file("hbase"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.hbase_conn_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .enablePlugins(JavaAppPackaging)

lazy val postgresConnector = Project("darwin-postgres-connector", file("postgres"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.postgres_conn_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .enablePlugins(JavaAppPackaging)

lazy val igniteConnector = Project("darwin-ignite-connector", file("ignite"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)

lazy val restConnector = Project("darwin-rest-connector", file("rest"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)

lazy val mongoConnector = Project("darwin-mongo-connector", file("mongo"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.core_deps)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)

lazy val mockConnector = Project("darwin-mock-connector", file("mock-connector"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(coreCommon)
  .settings(libraryDependencies ++= Dependencies.mock_app_dep)
  .settings(crossScalaVersions := Versions.crossScalaVersions)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)

lazy val mockApplication = Project("darwin-mock-application", file("mock-application"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(core, mockConnector)
  .settings(libraryDependencies ++= Dependencies.mock_app_dep)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)

lazy val sparkApplication = Project("darwin-spark-application", file("spark-application"))
  .settings(Settings.commonSettings:_*)
  .dependsOn(core, hbaseConnector)
  .settings(libraryDependencies ++= Dependencies.spark_app)
  .settings(Settings.notPublishSettings)
  .enablePlugins(JavaAppPackaging)
