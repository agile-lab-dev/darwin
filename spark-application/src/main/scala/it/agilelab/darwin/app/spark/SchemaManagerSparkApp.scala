package it.agilelab.darwin.app.spark

import com.typesafe.config.{Config, ConfigFactory}
import it.agilelab.darwin.app.spark.classes._
import it.agilelab.darwin.manager.AvroSchemaManager
import org.apache.avro.reflect.ReflectData
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}

object SchemaManagerSparkApp extends GenericMainClass with SparkManager {

  val mainLogger: Logger = LoggerFactory.getLogger("SchemaManagerSparkApp")

  override protected def runJob(settings: Config)(implicit fs: FileSystem, sparkSession: SparkSession): Int = {
    import sparkSession.implicits._

    val ds = sparkSession.createDataset(sparkSession.sparkContext.parallelize(1 to 1000, 20))
    mainLogger.info("Registering schemas")
    //    val reflections = new Reflections("it.agilelab.darwin.app.spark.classes")
    //    val annotationClass: Class[AvroSerde] = classOf[AvroSerde]
    //    val classes = reflections.getTypesAnnotatedWith(annotationClass).asScala.toSeq
    //      .filter(c => !c.isInterface && !Modifier.isAbstract(c.getModifiers))
    //    val schemas = classes.map(c => ReflectData.get().getSchema(Class.forName(c.getName)))
    val schemas = Seq(ReflectData.get().getSchema(classOf[Menu]), ReflectData.get().getSchema(classOf[MenuItem]),
      ReflectData.get().getSchema(classOf[Food]), ReflectData.get().getSchema(classOf[Order]),
      ReflectData.get().getSchema(classOf[Price]))
    val registeredIDs: Seq[Long] = AvroSchemaManager(ConfigFactory.load()).registerAll(schemas).map(_._1)
    ds.foreach(_ => AvroSchemaManager(ConfigFactory.load()))
    mainLogger.info("Schemas registered")

    mainLogger.info("Getting ID for a schema")
    AvroSchemaManager.getId(ReflectData.get().getSchema(classOf[Menu]))
    mainLogger.info("ID retrieved for the schema")

    mainLogger.info("Get Schema from ID")
    val d2 = ds.map { x =>
      AvroSchemaManager.getSchema(registeredIDs(x % registeredIDs.size))
      x
    }
    d2.count()
    mainLogger.info("All schemas obtained")

    10
  }

  override protected def handleException(exception: Throwable, applicationSettings: Config): Unit = {
    mainLogger.error(exception.getMessage)
  }
}
