package it.agilelab.darwin.connector.dynamo

import java.nio.ByteBuffer
import java.util

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{Connector, LONG_SIZE}
import org.apache.avro.Schema

class DynamoConnector(dynamoDBClient: AmazonDynamoDB, tableName: String) extends Connector {

  private[this] val ID = "id"
  private[this] val SCHEMA = "schema"
  private[this] val NAME = "name"
  private[this] val NAMESPACE = "namespace"

  private[this] def parser = new Schema.Parser()

  def this(connectorConfig: DynamoConnectorConfig, tableName: String) =
    this(DynamoConnectorConfig.toDynamoClientBuilder(connectorConfig).build(), tableName)

  /**
    * Creates the configured table, if the table already exists, does nothing
    */
  override def createTable(): Unit = {
    if (!tableExists) {
      dynamoDBClient.createTable(
        new CreateTableRequest(tableName, util.Arrays.asList(new KeySchemaElement(ID, KeyType.HASH)))
      )
    }
  }

  /**
    * Returns whether or not the configured table exists
    */
  override def tableExists(): Boolean = {
    try {
      dynamoDBClient.describeTable(tableName)
      true
    } catch {
      case _: ResourceNotFoundException => false
    }
  }

  /**
    *
    * @return a message indicating the user what he/she should do to create the table him/herself
    */
  override def tableCreationHint(): String = ""

  /**
    * Loads all schemas found on the storage.
    * This method can be invoked multiple times: to initialize the initial values or to update the existing ones with
    * the new data found on the storage.
    *
    * @return a sequence of all the pairs (ID, schema) found on the storage
    */
  override def fullLoad(): Seq[(Long, Schema)] = {
    val scan = dynamoDBClient.scan(tableName, util.Arrays.asList(ID, SCHEMA))
    scan.getItems.toScala().map { row =>
      row.get(ID).getB.getLong -> parser.parse(row.get(SCHEMA).getS)
    }.toSeq
  }

  /**
    * Inserts all the schema passed as parameters in the storage.
    * This method is called when new schemas should be registered in the storage (the test if a schema is already in
    * the storage should be performed before the invocation of this method, e.g. by checking them against the
    * pre-loaded cache).
    *
    * @param schemas a sequence of pairs (ID, schema) Schema entities to insert in the storage.
    */
  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    dynamoDBClient.batchWriteItem()
    schemas.foreach { case (k, v) =>
      val keys: util.Map[String, AttributeValue] = new util.HashMap[String, AttributeValue]() {
        put(ID, new AttributeValue().withB(ByteBuffer.allocate(LONG_SIZE).putLong(k)))
      }
      val values: util.Map[String, AttributeValueUpdate] = new util.HashMap[String, AttributeValueUpdate]() {
        put(SCHEMA, new AttributeValueUpdate(new AttributeValue(v.toString(false)), AttributeAction.PUT))
        put(NAME, new AttributeValueUpdate(new AttributeValue(v.getName), AttributeAction.PUT))
        put(NAMESPACE, new AttributeValueUpdate(new AttributeValue(v.getNamespace), AttributeAction.PUT))
      }
      val updateItemRequest = new UpdateItemRequest()
        .withTableName(tableName)
        .withKey(keys)
        .withAttributeUpdates(values)

      dynamoDBClient.updateItem(updateItemRequest)
    }
  }

  /**
    * Retrieves a single schema using its ID from the storage.
    *
    * @param id the ID of the schema
    * @return an option that is empty if no schema was found for the ID or defined if a schema was found
    */
  override def findSchema(id: Long): Option[Schema] = {
    val item = dynamoDBClient.getItem(new GetItemRequest(tableName, new util.HashMap[String, AttributeValue]() {
      put(ID, new AttributeValue().withB(ByteBuffer.allocate(LONG_SIZE).putLong(id)))
    }, true))
    Option(item.getItem).map { row =>
      parser.parse(row.get(SCHEMA).getS)
    }
  }
}
