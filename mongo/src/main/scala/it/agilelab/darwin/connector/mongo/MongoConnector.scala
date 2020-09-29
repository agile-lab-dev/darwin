package it.agilelab.darwin.connector.mongo

import com.mongodb.{ BasicDBObject, ErrorCategory }
import it.agilelab.darwin.common.{ Connector, Logging }
import it.agilelab.darwin.connector.mongo.ConfigurationMongoModels.BaseMongoConfig
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.mongodb.scala.bson.{ BsonDocument, BsonValue }
import org.mongodb.scala.{ bson, Document, MongoClient, MongoCollection, MongoWriteException }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.util.{ Failure, Try }

class MongoConnector(mongoClient: MongoClient, mongoConfig: BaseMongoConfig) extends Connector with Logging {

  private def parser: Parser = new Parser()

  override def fullLoad(): Seq[(Long, Schema)] = {

    log.debug(s"loading all schemas from collection ${mongoConfig.collection}")
    val collection =
      mongoClient
        .getDatabase(mongoConfig.database)
        .getCollection(mongoConfig.collection)

    val schemas: Seq[Try[(Long, Schema)]] =
      Await.result(
        collection
          .find()
          .map { document =>
            for {
              key       <- extract(document, "_id", _.asInt64().getValue)
              schemaStr <- extract(document, "schema", _.asString().getValue)
              schema    <- Try(parser.parse(schemaStr))
            } yield key -> schema
          }
          .toFuture(),
        mongoConfig.timeout
      )
    log.debug(s"${schemas.size} loaded from MongoDB")
    // this way the first exception is thrown, but we can change this line
    // to support different error handling strategies
    schemas.map(_.get)
  }

  private def extract[A](d: Document, fieldName: String, f: BsonValue => A): Try[A] = {
    d.filterKeys(k => k == fieldName)
      .headOption
      .fold[Try[A]](Failure(new RuntimeException(s"Cannot find $fieldName field in document"))) { case (_, value) =>
        Try(f(value)).recoverWith { case t: Throwable =>
          Failure(new RuntimeException(s"$fieldName was not of expected type", t))
        }
      }
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {

    log.debug(s"inclusion of new schemas in the collection ${mongoConfig.collection}")

    schemas.foreach { case (id, schema) =>
      val document = new BsonDocument
      document.put("_id", bson.BsonInt64(id))
      document.put("schema", bson.BsonString(schema.toString))
      document.put("name", bson.BsonString(schema.getName))
      document.put("namespace", bson.BsonString(schema.getNamespace))

      insertIfNotExists(mongoClient.getDatabase(mongoConfig.database).getCollection(mongoConfig.collection), document)
    }
  }

  private def insertIfNotExists(collection: MongoCollection[Document], document: BsonDocument): Unit = {
    try {
      Await.result(collection.insertOne(document).toFuture(), mongoConfig.timeout)
    } catch {
      case ex: MongoWriteException if ex.getError.getCategory == ErrorCategory.DUPLICATE_KEY =>
        log.info("document already present, doing nothing")
    }
    ()
  }

  override def createTable(): Unit = {
    log.debug(s"Creating collection ${mongoConfig.collection}")
    try {
      Await.result(
        mongoClient.getDatabase(mongoConfig.database).createCollection(mongoConfig.collection).toFuture(),
        mongoConfig.timeout
      )
      log.info(s"collection ${mongoConfig.collection} has been correctly created")
    } catch {
      case e: Exception => log.info(s"collection ${mongoConfig.collection} was not created. \n ${e.getMessage}")
    }
  }

  override def tableExists(): Boolean = {
    Await.result(
      mongoClient
        .getDatabase(mongoConfig.database)
        .listCollectionNames()
        .filter(x => x == mongoConfig.collection)
        .toFuture()
        .map(_.size),
      mongoConfig.timeout
    ) == 1
  }

  override def tableCreationHint(): String = {
    s"""To create the collection from shell perform the following command:
       |db.createCollection(${mongoConfig.collection})
     """.stripMargin
  }

  override def findSchema(id: Long): Option[Schema] = {

    val query = new BasicDBObject
    query.put("_id", bson.BsonInt64(id))

    val documents =
      mongoClient
        .getDatabase(mongoConfig.database)
        .getCollection(mongoConfig.collection)
        .find(query)
        .toFuture()

    val schemaValue: Seq[String] =
      for {
        document <- Await.result(documents, mongoConfig.timeout)
        field    <- document
        if field._1 == "schema"
      } yield field._2.asString().getValue
    schemaValue.headOption.map(parser.parse)
  }
}
