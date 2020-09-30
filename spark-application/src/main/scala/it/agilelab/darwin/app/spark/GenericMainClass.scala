package it.agilelab.darwin.app.spark

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.SparkSession
import org.slf4j.{ Logger, LoggerFactory }
import scala.collection.JavaConverters._

trait GenericMainClass {
  self: SparkManager =>

  val genericMainClassLogger: Logger = LoggerFactory.getLogger("SparkManager")

  private def makeFileSystem(session: SparkSession): FileSystem = {
    if (session.sparkContext.isLocal) {
      FileSystem.getLocal(session.sparkContext.hadoopConfiguration)
    } else {
      FileSystem.get(session.sparkContext.hadoopConfiguration)
    }
  }

  /**
    * @param settings     configuration loaded from multiple ".conf" files: the default ones as per typesafe Config and
    *                     another ".conf" file that has the same name as the application
    * @param fs           the default file system of the application executed context
    * @param sparkSession the sparkSession that has been created and will be used in the application
    * @return true if the application ends successfully false otherwise
    */
  protected def runJob(settings: Config)(implicit fs: FileSystem, sparkSession: SparkSession): Int

  /**
    * Override in order to handle specific exceptions
    */
  protected def handleException(exception: Throwable, applicationSettings: Config)

  /**
    * It executes the following ordered steps:
    * <ol>
    * <li>load the configuration</li>
    * <li>creates a SparkSession</li>
    * <li>instantiates a file system</li>
    * <li>logs the application start time</li>
    * <li>executes the [[runJob]] methods</li>
    * <li>logs the application end time</li>
    * <li>logs the application duration</li>
    * <li>stops the sparkSession</li>
    * </ol>
    * If an exception is thrown during step 5 calls the [[handleException]]
    */
  final def main(args: Array[String]): Unit = {
    val globalConfig = getGlobalConfig

    try {

      genericMainClassLogger.info(s"Creating SparkContext, Sqlcontext and FileSystem")
      val sparkSession = makeSparkSession(globalConfig)

      val fs = makeFileSystem(sparkSession)

      genericMainClassLogger.info("Starting application")
      val startTime         = System.currentTimeMillis()
      val end               =
        runJob(globalConfig)(fs, sparkSession)
      val exectime          = System.currentTimeMillis() - startTime
      val exectimeFormatted = new SimpleDateFormat("mm:ss:SSS").format(new Date(exectime))
      if (end >= 0) {
        genericMainClassLogger.info(s"Execution finished in [$exectimeFormatted]")
        genericMainClassLogger.info("Closing application")
        sparkSession.stop()
      } else {
        genericMainClassLogger.info(s"Execution stopped after [$exectimeFormatted]")
        System.exit(end)
      }
    } catch {
      case e: Throwable =>
        genericMainClassLogger.error(e.getMessage)
        handleException(e, globalConfig)
        throw e
    }
  }

  /**
    * Loads env vars, system properties and the "spark.app.name".conf config file, returning all of them as
    * [[Config]] object.
    *
    * By launching a Spark process with the spark-submit tool and specifying with "--name MyAppName", the system
    * automatically drives itself to look for a MyAppName.conf file in the classpath and tries to load it as [[Config]].
    *
    * If no such file is provided/found (it could happen also by declaring a wrong --name arg, the system falls back
    * to framework's default configuration parameters.
    *
    * @return a Typesafe [[Config]] object
    */
  // scalastyle:off
  private def getGlobalConfig: Config = {
    genericMainClassLogger.debug("system environment vars")
    for ((k, v) <- System.getenv().asScala.toSeq.sortBy(_._1)) genericMainClassLogger.debug(s"$k -> $v")

    genericMainClassLogger.debug("system properties")
    for ((k, v) <- System.getProperties.asScala.toSeq.sortBy(_._1)) genericMainClassLogger.debug(s"$k -> $v")

    ConfigFactory.load()
  }

  // scalastyle:on

}
