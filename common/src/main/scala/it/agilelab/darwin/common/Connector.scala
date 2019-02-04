package it.agilelab.darwin.common

import org.apache.avro.Schema

/**
  * Generic abstraction of a component capable of reading and writing Schema entities in an external storage.
  * The external storage should keep at least the ID (Long) and the schema (Schema) for each entry.
  */
trait Connector extends Serializable {

  /**
    * Creates the configured table, if the table already exists, does nothing
    */
  def createTable(): Unit

  /**
    * Returns whether or not the configured table exists
    */
  def tableExists(): Boolean

  /**
    *
    * @return a message indicating the user what he/she should do to create the table him/herself
    */
  def tableCreationHint(): String

  /**
    * Loads all schemas found on the storage.
    * This method can be invoked multiple times: to initialize the initial values or to update the existing ones with
    * the new data found on the storage.
    *
    * @return a sequence of all the pairs (ID, schema) found on the storage
    */
  def fullLoad(): Seq[(Long, Schema)]

  /**
    * Inserts all the schema passed as parameters in the storage.
    * This method is called when new schemas should be registered in the storage (the test if a schema is already in
    * the storage should be performed before the invocation of this method, e.g. by checking them against the
    * pre-loaded cache).
    *
    * @param schemas a sequence of pairs (ID, schema) Schema entities to insert in the storage.
    */
  def insert(schemas: Seq[(Long, Schema)]): Unit

  /**
    * Retrieves a single schema using its ID from the storage.
    *
    * @param id the ID of the schema
    * @return an option that is empty if no schema was found for the ID or defined if a schema was found
    */
  def findSchema(id: Long): Option[Schema]

  def findSchemasByName(name: String): Seq[Schema]

  def findSchemasByNamespace(namespace: String): Seq[Schema]

  def findSchemaByNameAndNamespace(name: String, namespace: String): Seq[Schema]
}
