package it.agilelab.darwin.common

import java.io.{ InputStream, OutputStream }
import java.nio.{ ByteBuffer, ByteOrder }

import it.agilelab.darwin.common.compat.RightBiasedEither
import it.agilelab.darwin.manager.SchemaPayloadPair
import it.agilelab.darwin.manager.exception.DarwinException
import it.agilelab.darwin.manager.util.AvroSingleObjectEncodingUtils
import org.apache.avro.{ Schema, SchemaNormalization }

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

  /**
   * Retrieves the latest schema for a given string identifier (not to be confused with the fingerprint id).
   * This API might not be implemented by all connectors, which should return None
   */
  def retrieveLatestSchema(identifier: String): Option[(Long, Schema)]

  /**
    * Generate a fingerprint for a schema, the default implementation is SchemaNormalization.parsingFingerprint64
    *
    * @param schema the schema to fingerprint
    * @return the schema id
    */
  def fingerprint(schema: Schema): Long = {
    SchemaNormalization.parsingFingerprint64(schema)
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header and returns the OutputStream
    *
    * @return the input OutputStream
    */
  def writeHeaderToStream(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder): OutputStream = {
    AvroSingleObjectEncodingUtils.writeHeaderToStream(byteStream, schemaId, endianness)
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
  def generateAvroSingleObjectEncoded(
    avroPayload: Array[Byte],
    schema: Schema,
    endianness: ByteOrder,
    getId: Schema => Long
  ): Array[Byte] = {
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(avroPayload, getId(schema), endianness)
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header then the avroValue and returns the OutputStream
    *
    * @param byteStream the stream to write to
    * @param avroValue  the value to be written to the stream
    * @param schemaId   id of the schema used to encode the payload
    * @return the input OutputStream
    */
  def generateAvroSingleObjectEncoded(
    byteStream: OutputStream,
    avroValue: Array[Byte],
    schemaId: Long,
    endianness: ByteOrder
  ): OutputStream = {
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(byteStream, avroValue, schemaId, endianness)
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
  def generateAvroSingleObjectEncoded(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder)(
    avroWriter: OutputStream => OutputStream
  ): OutputStream = {
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(byteStream, schemaId, endianness)(avroWriter)
  }

  /**
    * Extracts a Tuple2 that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a pair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndAvroPayload(
    avroSingleObjectEncoded: Array[Byte],
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): (Schema, Array[Byte]) = {
    if (AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      val id = extractId(avroSingleObjectEncoded, endianness)
      getSchema(id) match {
        case Some(schema) =>
          schema -> AvroSingleObjectEncodingUtils.dropHeader(avroSingleObjectEncoded)
        case _            =>
          throw new DarwinException(s"No schema found for ID $id")
      }
    } else {
      throw AvroSingleObjectEncodingUtils.parseException()
    }
  }

  /**
    * Extracts the Schema from the ByteBuffer after the method call the ByteBuffer position will be right after the
    * header.
    *
    * @param avroSingleObjectEncoded a ByteBuffer of a Single-Object encoded payload
    * @return the avro Schema
    */
  def retrieveSchemaAndAvroPayload(
    avroSingleObjectEncoded: ByteBuffer,
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): Schema = {
    if (AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      val id = extractId(avroSingleObjectEncoded, endianness)
      getSchema(id) match {
        case Some(schema) => schema
        case _            => throw new DarwinException(s"No schema found for ID $id")
      }
    } else {
      throw AvroSingleObjectEncodingUtils.parseException()
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
  def extractSchema(
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
  def extractSchema(
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
   * Extracts the schema ID from the avro single-object encoded byte array
   *
   * @param array avro single-object encoded byte array
   * @param endianness              the endianness that will be used to read fingerprint bytes,
   *                                it won't affect how avro payload is read, that is up to the darwin user
   * @return the schema ID extracted from the input data
   */
  def extractId(array: Array[Byte], endianness: ByteOrder): Long = {
    AvroSingleObjectEncodingUtils.extractId(array, endianness)
  }

  /**
   * Extracts the schema ID from the avro single-object encoded at the head of this input stream.
   * The input stream will have 10 bytes consumed if the first two bytes correspond to the single object encoded
   * header, or zero bytes consumed if the InputStream supports marking; if it doesn't, the first bytes (up to 2) will
   * be consumed and returned in the Left part of the Either.
   *
   * @param inputStream avro single-object encoded input stream
   * @param endianness  the endianness that will be used to read fingerprint bytes,
   *                    it won't affect how avro payload is read, that is up to the darwin user
   * @return the schema ID extracted from the input data
   */
  def extractId(inputStream: InputStream, endianness: ByteOrder): Either[Array[Byte], Long] = {
    AvroSingleObjectEncodingUtils.extractId(inputStream, endianness)
  }

  /**
   * Extracts the schema ID from the avro single-object encoded ByteBuffer, the ByteBuffer position will be after the
   * header when this method returns
   *
   * @param avroSingleObjectEncoded avro single-object encoded byte array
   * @param endianness              the endianness that will be used to read fingerprint bytes,
   *                                it won't affect how avro payload is read, that is up to the darwin user
   * @return the schema ID extracted from the input data
   */
  def extractId(avroSingleObjectEncoded: ByteBuffer, endianness: ByteOrder): Long = {
    AvroSingleObjectEncodingUtils.extractId(avroSingleObjectEncoded, endianness)
  }

  /**
    * Extracts a SchemaPayloadPair that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a SchemaPayloadPair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndPayload(
    avroSingleObjectEncoded: Array[Byte],
    endianness: ByteOrder,
    getSchema: Long => Option[Schema]
  ): SchemaPayloadPair = {
    val (schema, payload) = retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
    SchemaPayloadPair.create(schema, payload)
  }
}
