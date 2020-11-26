package it.agilelab.darwin.connector.confluent

import java.io.{ InputStream, OutputStream }
import java.nio.{ ByteBuffer, ByteOrder }

import io.confluent.kafka.schemaregistry.client.{ SchemaMetadata, SchemaRegistryClient }
import it.agilelab.darwin.common.Connector
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.manager.SchemaPayloadPair
import it.agilelab.darwin.manager.exception.DarwinException
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

  /**
    * Writes to the given OutputStream the Single Object Encoding header and returns the OutputStream
    *
    * @return the input OutputStream
    */
  override def writeHeaderToStream(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder): OutputStream = {
    ConfluentSingleObjectEncoding.writeHeaderToStream(byteStream, schemaId, endianness)
  }

  /**
    * Create an array that creates a Single-Object encoded byte array.
    * By specifications the encoded array is obtained concatenating the V1_HEADER, the schema id and the avro-encoded
    * payload.
    *
    * @param avroPayload avro-serialized payload
    * @param schema      the schema used to encode the payload
    * @return a Single-Object encoded byte array
    */
  override def generateAvroSingleObjectEncoded(
    avroPayload: Array[Byte],
    schema: Schema,
    endianness: ByteOrder,
    getId: Schema => Long
  ): Array[Byte] = {
    ConfluentSingleObjectEncoding.generateAvroSingleObjectEncoded(avroPayload, getId(schema), endianness)
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header then the avroValue and returns the OutputStream
    *
    * @param byteStream the stream to write to
    * @param avroValue  the value to be written to the stream
    * @param schemaId   id of the schema used to encode the payload
    * @return the input OutputStream
    */
  override def generateAvroSingleObjectEncoded(
    byteStream: OutputStream,
    avroValue: Array[Byte],
    schemaId: Long,
    endianness: ByteOrder
  ): OutputStream = {
    ConfluentSingleObjectEncoding.generateAvroSingleObjectEncoded(byteStream, avroValue, schemaId, endianness)
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header then calls the avroWriter function to
    * possibly add data to the stream and finally returns the OutputStream
    *
    * @param byteStream the stream to write to
    * @param schemaId   id of the schema used to encode the payload
    * @param avroWriter function that will be called to add user generated avro to the stream
    * @return the input OutputStream
    */
  override def generateAvroSingleObjectEncoded(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder)(
    avroWriter: OutputStream => OutputStream
  ): OutputStream = {
    ConfluentSingleObjectEncoding.generateAvroSingleObjectEncoded(byteStream, schemaId, endianness)(avroWriter)
  }

  /**
    * Extracts a Tuple2 that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a pair containing the Schema and the payload of the input array
    */
  override def retrieveSchemaAndAvroPayload(
    avroSingleObjectEncoded: Array[Byte],
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): (Schema, Array[Byte]) = {
    if (ConfluentSingleObjectEncoding.isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      val id = extractId(avroSingleObjectEncoded, endianness)
      getSchema(id) match {
        case Some(schema) =>
          schema -> ConfluentSingleObjectEncoding.dropHeader(avroSingleObjectEncoded)
        case _            =>
          throw new DarwinException(s"No schema found for ID $id")
      }
    } else {
      throw ConfluentSingleObjectEncoding.parseException()
    }
  }

  /**
    * Extracts the Schema from the ByteBuffer after the method call the ByteBuffer position will be right after the
    * header.
    *
    * @param avroSingleObjectEncoded a ByteBuffer of a Single-Object encoded payload
    * @return the avro Schema
    */
  override def retrieveSchemaAndAvroPayload(
    avroSingleObjectEncoded: ByteBuffer,
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): Schema = {
    if (ConfluentSingleObjectEncoding.isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      val id = extractId(avroSingleObjectEncoded, endianness)
      getSchema(id) match {
        case Some(schema) => schema
        case _            => throw new DarwinException(s"No schema found for ID $id")
      }
    } else {
      throw ConfluentSingleObjectEncoding.parseException()
    }
  }

  /**
    * Extracts the schema from the avro single-object encoded at the head of this input stream.
    * The input stream will have 10 bytes consumed if the first two bytes correspond to the single object encoded
    * header, or zero bytes consumed if the InputStream supports marking; if it doesn't, the first bytes (up to 2) will
    * be consumed and returned in the Left part of the Either
    *
    * @param inputStream avro single-object encoded input stream
    * @return the schema ID extracted from the input data
    */
  override def extractSchema(
    inputStream: InputStream,
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): Either[Array[Byte], Schema] = {
    extractId(inputStream, endianness).rightMap { id =>
      getSchema(id).getOrElse(throw new DarwinException(s"No schema found for ID $id"))
    }
  }

  /**
    * Extracts the schema from the avro single-object encoded in the input array.
    *
    * @param array avro single-object encoded array
    * @return the schema ID extracted from the input data
    */
  override def extractSchema(
    array: Array[Byte],
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): Either[Exception, Schema] = {
    try {
      val id = extractId(array, endianness)
      getSchema(id)
        .toRight(new RuntimeException(s"Cannot find schema with id $id"))
    } catch {
      case ie: IllegalArgumentException => Left(ie)
    }
  }

  /**
    * Extracts a SchemaPayloadPair that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a SchemaPayloadPair containing the Schema and the payload of the input array
    */
  override def retrieveSchemaAndPayload(
    avroSingleObjectEncoded: Array[Byte],
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): SchemaPayloadPair = {
    val (schema, payload) = retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
    SchemaPayloadPair.create(schema, payload)
  }

  override def extractId(array: Array[Byte], endianness: ByteOrder): Long = {
    ConfluentSingleObjectEncoding.extractId(array, endianness)
  }

  override def extractId(inputStream: InputStream, endianness: ByteOrder): Either[Array[Byte], Long] = {
    ConfluentSingleObjectEncoding.extractId(inputStream, endianness)
  }

  override def extractId(avroSingleObjectEncoded: ByteBuffer, endianness: ByteOrder): Long = {
    ConfluentSingleObjectEncoding.extractId(avroSingleObjectEncoded, endianness)
  }
}
