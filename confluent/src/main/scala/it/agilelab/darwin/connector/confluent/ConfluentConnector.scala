package it.agilelab.darwin.connector.confluent

import io.confluent.kafka.schemaregistry.client.{ SchemaMetadata, SchemaRegistryClient }
import it.agilelab.darwin.common.Connector
import it.agilelab.darwin.common.compat._
import org.apache.avro.Schema

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

    client.getAllSubjects.toScala().toList.flatMap { subject =>
      val versions = client.getAllVersions(subject).toScala().toList

      versions.map { version =>
        val metadata = client.getSchemaMetadata(subject, version)

        val id: Long       = metadata.getId.toLong
        val schema: Schema = new Schema.Parser().parse(metadata.getSchema)

        (id, schema)
      }
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
    //registration happens during fingerprinting
  }

  /**
    * Retrieves a single schema using its ID from the storage.
    *
    * @param id the ID of the schema
    * @return an option that is empty if no schema was found for the ID or defined if a schema was found
    */
  override def findSchema(id: Long): Option[Schema] = {
    Option(client.getById(id.toInt))
  }

  override def fingerprint(schema: Schema): Long = {
    val subject = Option(schema.getProp("x-darwin-subject"))

    client.register(
      subject.getOrElse(throw new IllegalArgumentException("Schema does not contain the [x-darwin-subject] extension")),
      schema
    )
  }

  def findVersionsForSubject(subject: String): Seq[Integer] = {
    client.getAllVersions(subject).toScala().toList
  }

  def findIdForSubjectVersion(subject: String, version: Int): SchemaMetadata = {
    client.getSchemaMetadata(subject, version)
  }

  def findIdForSubjectLatestVersion(subject: String): SchemaMetadata = {
    client.getLatestSchemaMetadata(subject)
  }
}
