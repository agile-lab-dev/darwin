package it.agilelab.darwin.connector.mongo

import java.util

import avro.shaded.com.google.common.primitives.Bytes
import com.mongodb.{BasicDBObject, MongoClientSettings, MongoCredential, ServerAddress}
import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, Logging}
import org.apache.avro.Schema
import com.mongodb.MongoCredential._
import com.mongodb.client.model.Projections
import org.apache.avro.Schema.Parser
import org.bson.{BsonString, BsonValue}
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{Document, FindObservable, ListCollectionsObservable, MongoClient, MongoDatabase, Observable, SingleObservable, bson}

import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.bson.Document


case class MongoConfig(user: String, password: String, hosts: List[String], db: String, table: String)

class MongoConnector(config: Config) extends Connector with Logging {

  require(config.hasPath(ConfigurationKeys.USER))
  require(config.hasPath(ConfigurationKeys.DATABASE))
  require(config.hasPath(ConfigurationKeys.PASSWORD))
  require(config.hasPath(ConfigurationKeys.HOST))
  require(config.hasPath(ConfigurationKeys.TABLE))

  log.debug("Creating default MongoConfiguration")
  val mongoConfig: MongoConfig = MongoConfig(
    config.getString(ConfigurationKeys.USER),
    config.getString(ConfigurationKeys.PASSWORD),
    config.getStringList(ConfigurationKeys.HOST).asScala.toList,
    config.getString(ConfigurationKeys.DATABASE),
    config.getString(ConfigurationKeys.TABLE)
  )
  log.debug("Created default MongoConfiguration")

  log.debug(s"Creating connections")
  val mongoClient: MongoClient = createConnection(mongoConfig)
  log.debug(s"Connection created")

  //  In MongoDB tables are called collections.
  //  Schema:
  //    id:        idSchema         (long)
  //    schema:    schema           (string)
  //    name:      schema.name      (string)
  //    namespace: schema.namespace (string)

  /**
   * return the MongoClient
   * @param mongoConf : config with keys defined by ConfigurationKeys object
   * @return MongoClient
   */
  private def createConnection(mongoConf: MongoConfig): MongoClient = {

    val credential: MongoCredential = createCredential(mongoConf.user, mongoConf.db, mongoConf.password.toCharArray)

    val hosts: List[ServerAddress] = mongoConf.hosts.map(host => new ServerAddress(host))

    val settings: MongoClientSettings = MongoClientSettings.builder()
      .credential(credential)
      .applyToClusterSettings(builder => builder.hosts(hosts.asJava))
      //.streamFactoryFactory(NettyStreamFactoryFactory())
      .build()

    MongoClient(settings)

  }

  private def parser: Parser = new Parser()

  override def fullLoad(): Seq[(Long, Schema)] = {
    log.debug(s"loading all schemas from table ${mongoConfig.table}")
    val collectionNames =
      mongoClient
        .getDatabase(mongoConfig.db)
        .listCollectionNames()

    val schemas: Seq[(Long, Schema)] =
      Await.result(
        collectionNames.flatMap(collectionName =>
          mongoClient.getDatabase(mongoConfig.db).getCollection(collectionName).find().map(document => {
            val key = document.filterKeys(k => k == "id").head._2.asInt64().getValue
            val schema = parser.parse(document.filterKeys(k => k == "schema").head._2.asString().getValue)
            key -> schema
          })
        ).toFuture()
        ,
        Duration.Undefined
      )

    log.debug(s"${schemas.size} loaded from MongoDB")
    schemas
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {

    schemas.foreach{
      case (id, schema) => {
        val document = new BsonDocument
        document.put("id", bson.BsonInt64(id))
        document.put("schema", bson.BsonString(schema.toString))
        document.put("name", bson.BsonString(schema.getName))
        document.put("namespace", bson.BsonString(schema.getNamespace))

        mongoClient.getDatabase(mongoConfig.db).getCollection(mongoConfig.table).insertOne(document)
      }
    }

  }

  override def createTable(): Unit = {
    log.debug(s"Creating table ${mongoConfig.table}")
    mongoClient.getDatabase(mongoConfig.db).createCollection(mongoConfig.table)
    log.debug(s"Table ${mongoConfig.table} created")
  }

  override def tableExists(): Boolean = {
    Await.result(
      mongoClient
        .getDatabase(mongoConfig.db)
        .listCollectionNames()
        .filter(x => x == mongoConfig.table)
        .toFuture().map(_.size)
      ,
      Duration.Undefined
    ) == 1
  }

  override def tableCreationHint(): String = {
    s"""To create table perform the following command:
       |mongoClient.getDatabase(<DATABASE>).createCollection(<TABLE>)
     """.stripMargin
  }

  override def findSchema(id: Long): Option[Schema] = {

    val query = new BasicDBObject
    query.put("id", id)

    val document =
      mongoClient
        .getDatabase(mongoConfig.db)
        .getCollection(mongoConfig.table)
        .find(query)
        .toFuture()

    // at max there is only one schema value (string)
    val schemaValue = Await.result(document, Duration.Undefined).flatMap(documents => documents.map(doc => doc._2.asString().getValue))

    if (schemaValue.nonEmpty) {
      Some(parser.parse(schemaValue.head))
    } else {
      None
    }
  }

}
