package it.agilelab.darwin.connector.mongo

import com.mongodb.{BasicDBObject, ErrorCategory}
import it.agilelab.darwin.common.{Connector, Logging}
import it.agilelab.darwin.connector.mongo.ConfigurationMongoModels.BaseMongoConfig
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoWriteException, bson}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.util.{Failure, Success}

class MongoConnector(mongoClient: MongoClient, mongoConfig: BaseMongoConfig) extends Connector with Logging {

  private def parser: Parser = new Parser()

  override def fullLoad(): Seq[(Long, Schema)] = {

    log.debug(s"loading all schemas from collection ${mongoConfig.collection}")
    val collection =
      mongoClient
        .getDatabase(mongoConfig.database)
        .getCollection(mongoConfig.collection)

    val schemas: Seq[(Long, Schema)] =
      Await.result(
        collection.find().map(document => {
          // usare option per tirare l'eccezione se non trova la key
          val key = document.filterKeys(k => k == "_id").head._2.asInt64().getValue
          val schema = parser.parse(document.filterKeys(k => k == "schema").head._2.asString().getValue)
          key -> schema
        }).toFuture(),
        mongoConfig.timeout
      )
    log.debug(s"${schemas.size} loaded from MongoDB")
    schemas
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {

    log.debug(s"inclusion of new schemas in the collection ${mongoConfig.collection}")

    schemas.foreach {
      case (id, schema) =>
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
    Unit
  }

  override def createTable(): Unit = {
    log.debug(s"Creating collection ${mongoConfig.collection}")
    val result = mongoClient.getDatabase(mongoConfig.database).createCollection(mongoConfig.collection)
    result.toFuture().onComplete {
      case Success(s) => log.info(s"collection ${mongoConfig.collection} has been correctly created. \n ${s.toString()}")
      case Failure(t) => log.info(s"collection ${mongoConfig.collection} was not created. \n ${t.getMessage}")
    }
  }

  override def tableExists(): Boolean = {
    Await.result(
      mongoClient
        .getDatabase(mongoConfig.database)
        .listCollectionNames()
        .filter(x => x == mongoConfig.collection)
        .toFuture().map(_.size),
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

    // at max there is only one schema value (string)
    val schemaValue: Seq[String] =
      Await.result(documents, mongoConfig.timeout)
        .flatMap(document =>
          document.flatMap(field =>
            if (field._1 == "schema") {
              Some(field._2.asString().getValue)
            }
            else {
              None
            })
        )

    if (schemaValue.nonEmpty) {
      Some(parser.parse(schemaValue.head))
    } else {
      None
    }
  }
}
