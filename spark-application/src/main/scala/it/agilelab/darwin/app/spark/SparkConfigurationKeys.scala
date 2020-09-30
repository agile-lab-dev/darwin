package it.agilelab.darwin.app.spark

trait SparkConfigurationKeys {
  val SPARK_APP_NAME            = "spark.app.name"
  val SPARK_CORES               = "spark.executor.cores"
  val PARALLELISM: String       = "parallelism"
  val SPARK_DRIVER_CORES        = "spark.driver.cores"
  val SPARK_EXECUTOR_INSTANCES  = "spark.executor.instances"
  val SPARK_DEFAULT_PARALLELISM = "spark.default.parallelism"
}

object SparkConfigurationKeys extends SparkConfigurationKeys
