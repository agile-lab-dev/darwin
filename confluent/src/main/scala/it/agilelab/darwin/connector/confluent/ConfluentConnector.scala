package it.agilelab.darwin.connector.confluent

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

import scala.collection.JavaConverters.asScalaBufferConverter

class ConfluentConnector(options: ConfluentConnectorOptions, client: SchemaRegistryClient) extends Connector {

  /**
    * Creates the configured table, if the table already exists, does nothing
    */
  override def createTable(): Unit = {}

  /**
    * Returns whether or not the configured table exists
    */
  override def tableExists(): Boolean = true

  /**
    * @return a message indicating the user what he/she should do to create the table him/herself
    */
  override def tableCreationHint(): String = "No need to create a table"

  /**
    * Loads all schemas found on the storage.
    * This method can be invoked multiple times: to initialize the initial values or to update the existing ones with
    * the new data found on the storage.
    *
    * @return a sequence of all the pairs (ID, schema) found on the storage
    */
  override def fullLoad(): Seq[(Long, Schema)] = {
    val versions = client.getAllVersions(options.subject).asScala.toList

    versions.map { version =>
      val metadata = client.getSchemaMetadata(options.subject, version)

      val id: Long       = metadata.getId.toLong
      val schema: Schema = new Schema.Parser().parse(metadata.getSchema)

      (id, schema)
    }
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
    schemas.foreach { case (id, schema) => client.register(options.subject, schema) }
  }

  /**
    * Retrieves a single schema using its ID from the storage.
    *
    * @param id the ID of the schema
    * @return an option that is empty if no schema was found for the ID or defined if a schema was found
    */
  override def findSchema(id: Long): Option[Schema] = {
    Option(client.getByID(id.toInt))
  }
}
