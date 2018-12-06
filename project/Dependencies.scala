import sbt._


/**
  * @author andreaL
  */
object Dependencies {

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.4" % "test"
  val avro = "org.apache.avro" % "avro" % "1.8.2"
  val typesafe_config = "com.typesafe" % "config" % "1.3.1"
  val hbase_server = "org.apache.hbase" % "hbase-server" % "1.2.0" % "provided"
  val hbase_common = "org.apache.hbase" % "hbase-common" % "1.2.0" % "provided"
  val hadoop_common = "org.apache.hadoop" % "hadoop-common" % "2.6.0" % "provided"
  val reflections = "org.reflections" % "reflections" % "0.9.11"
  val spark_core = "org.apache.spark" %% "spark-core" % "2.3.0" % "provided"
  val spark_sql = "org.apache.spark" %% "spark-sql" % "2.3.0" % "provided"
  val postgres_conn = "org.postgresql" % "postgresql" % "9.3-1100-jdbc4"
  val junit = "org.junit.jupiter" % "junit-jupiter-api" % "5.3.2" % Test
  
  val core_deps = Seq(scalatest, avro, typesafe_config, junit)
  val mock_app_dep = core_deps ++ Seq(reflections, hbase_common)
  val mock_conn = core_deps ++ Seq(reflections)
  val hbase_conn_dep = core_deps ++ Seq(hbase_common, hbase_server, hadoop_common)
  val postgres_conn_dep = core_deps :+ postgres_conn
  val spark_app = mock_app_dep ++ Seq(spark_core, spark_sql, hbase_common)
}
