package it.agilelab.darwin.app.spark

import com.typesafe.config.Config
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

trait SparkManager {

  val sparkManagerLogger: Logger = LoggerFactory.getLogger("SparkManager")

  /**
    * @return a SparkConf given the settings
    */
  protected def createSparkConf(settings: Config): SparkConf = {
    // Add conf file configurations
    val sparkSettings =
      if (settings.hasPath("spark")) {
        settings.getConfig("spark").entrySet().asScala.map(e => ("spark." + e.getKey, e.getValue.unwrapped().toString))
      } else {
        Seq()
      }
    // Add hbase related hadoop confs
    val hconfs = HBaseConfiguration.create().asScala.map { entry =>
      "spark.hadoop." + entry.getKey -> entry.getValue
    }
    new SparkConf()
      // Use spark.app.name to set the spark app name
      .setAll(hconfs)
      .setAll(sparkSettings)
  }

  private def withSparkConf(settings: Config)(f: SparkConf => SparkSession): SparkSession = f(
    {
      createSparkConf(settings)
    }
  )

  /**
    * @return a SparkSession given the settings
    */
  protected def makeSparkSession(settings: Config): SparkSession = withSparkConf(settings) {
    conf =>
      SparkSession.builder()
        .config(conf)
        .getOrCreate()
  }

  /**
    * @return the default Spark parallelism given the sparkSession and the config.
    *         It tries to infer it from the SparkSession, if it is not possible, it gathers it from the Config
    */
  protected def defaultParallelism(implicit sparkSession: SparkSession, config: Config): Int = {
    sparkSession.conf.getOption(SparkConfigurationKeys.SPARK_EXECUTOR_INSTANCES) match {
      case Some(instances) =>
        sparkSession.conf.getOption(SparkConfigurationKeys.SPARK_CORES).getOrElse("1").toInt * instances.toInt
      case None =>
        sparkManagerLogger.info("Spark is configured with dynamic allocation, default parallelism will be gathered from app " +
          "conf: " +
          "next.process.parallelism")
        if (config.hasPath(SparkConfigurationKeys.PARALLELISM)) {
          config.getInt(SparkConfigurationKeys.PARALLELISM)
        } else {
          sparkManagerLogger.info("next.process.parallelism was not set fallback to sparkSession.defaultParallelism")
          sparkSession.sparkContext.defaultParallelism
        }
    }
  }
}
