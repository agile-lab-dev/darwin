import sbt._


/**
  * @author andreaL
  */
object Dependencies {

  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  lazy val avro = "org.apache.avro" % "avro" % "1.8.2"
  lazy val typesafe_config = "com.typesafe" % "config" % "1.3.1"
  lazy val avro4s = "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.3"
  lazy val hbase_server = "org.apache.hbase" % "hbase-server" % "1.2.0" % "provided"
  lazy val hbase_common = "org.apache.hbase" % "hbase-common" % "1.2.0" % "provided"
  lazy val hadoop_common = "org.apache.hadoop" % "hadoop-common" % "2.6.0" % "provided"
  lazy val reflections = "org.reflections" % "reflections" % "0.9.11"
  lazy val spark_core = "org.apache.spark" %% "spark-core" % "2.3.0" % "provided"
  lazy val spark_sql = "org.apache.spark" %% "spark-sql" % "2.3.0" % "provided"
  lazy val postgres_conn = "org.postgresql" % "postgresql" % "9.3-1100-jdbc4"
  lazy val junit = "org.junit.jupiter" % "junit-jupiter-api" % "5.3.2" % Test


  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-stream" % "2.5.20",
    "com.typesafe.akka" %% "akka-slf4j" % "2.5.20",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.20" % Test,
    "com.typesafe.akka" %% "akka-http" % "10.1.7",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test,
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
  )

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val restServer = core_deps ++ Seq(logback) ++ akka
  lazy val core_deps = Seq(scalatest, avro, typesafe_config, junit)
  lazy val mock_app_dep = core_deps ++ Seq(reflections, hbase_common)
  lazy val mock_conn = core_deps ++ Seq(reflections)
  lazy val hbase_conn_dep = core_deps ++ Seq(hbase_common, hbase_server, hadoop_common)
  lazy val postgres_conn_dep = core_deps :+ postgres_conn
  lazy val spark_app = mock_app_dep ++ Seq(spark_core, spark_sql, hbase_common)
}
