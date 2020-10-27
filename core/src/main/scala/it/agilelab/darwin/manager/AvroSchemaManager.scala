package it.agilelab.darwin.manager

import java.io.{ InputStream, OutputStream }
import java.nio.{ ByteBuffer, ByteOrder }

import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{ Connector, Logging }
import it.agilelab.darwin.manager.util.AvroSingleObjectEncodingUtils
import org.apache.avro.Schema

/**
  * The main entry point of the Darwin library.
  * An instance of AvroSchemaManager should ALWAYS be obtained through the AvroSchemaManagerFactory.
  * The manager is responsible for schemas registration, retrieval and updates.
  *
  * @param connector  the connector used to retrieve and persist schemas
  * @param endianness the endianness that will be used to persist and parse fingerprint bytes, it won't affect how avro
  *                   payload is written, that is up to the darwin user
  */
abstract class AvroSchemaManager(connector: Connector, endianness: ByteOrder) extends Logging {

  /**
    * @return the configured endianness of this AvroSchemaManager instance
    */
  def getEndianness(): ByteOrder = endianness

  /**
    * Retrieves all registered schemas
    *
    * @return A Sequence of (ID, Schema)
    */
  def getAll: Seq[(Long, Schema)]

  /**
    * Extracts the ID from a Schema.
    *
    * @param schema a Schema with unknown ID
    * @return the ID associated with the input schema
    */
  def getId(schema: Schema): Long = AvroSingleObjectEncodingUtils.getId(schema, connector.fingerprint)

  /**
    * Extracts the Schema from its ID.
    *
    * @param id a Long representing an ID
    * @return the Schema associated to the input ID
    */
  def getSchema(id: Long): Option[Schema]

  /**
    * Checks if all the input Schema elements are already in the cache. Then, it performs an insert on the
    * storage for all the elements not found on the cache, and then returns each input schema paired with its ID.
    *
    * @param schemas all the Schema that should be registered
    * @return a sequence of pairs of the input schemas associated with their IDs
    */
  def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)]

  /**
    * JAVA API: Checks if all the input Schema elements are already in the cache. Then, it performs an insert on the
    * storage for all the elements not found on the cache, and then returns each input schema paired with its ID.
    *
    * @param schemas all the Schema that should be registered
    * @return a sequence of pairs of the input schemas associated with their IDs
    */
  def registerAll(schemas: java.lang.Iterable[Schema]): java.lang.Iterable[IdSchemaPair] = {
    registerAll(schemas.toScala().toSeq).map { case (id, schema) => IdSchemaPair.create(id, schema) }.toJava()
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header and returns the OutputStream
    *
    * @return the input OutputStream
    */
  def writeHeaderToStream(byteStream: OutputStream, schemaId: Long): OutputStream = {
    connector.writeHeaderToStream(byteStream, schemaId, endianness)
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
  def generateAvroSingleObjectEncoded(avroPayload: Array[Byte], schema: Schema): Array[Byte] = {
    connector.generateAvroSingleObjectEncoded(avroPayload, schema, endianness, getId)
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
    schemaId: Long
  ): OutputStream = {
    connector.generateAvroSingleObjectEncoded(byteStream, avroValue, schemaId, endianness)
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
  def generateAvroSingleObjectEncoded(byteStream: OutputStream, schemaId: Long)(
    avroWriter: OutputStream => OutputStream
  ): OutputStream = {
    connector.generateAvroSingleObjectEncoded(byteStream, schemaId, endianness)(avroWriter)
  }

  /**
    * Extracts a Tuple2 that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a pair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndAvroPayload(avroSingleObjectEncoded: Array[Byte]): (Schema, Array[Byte]) = {
    connector.retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
  }

  /**
    * Extracts the Schema from the ByteBuffer after the method call the ByteBuffer position will be right after the
    * header.
    *
    * @param avroSingleObjectEncoded a ByteBuffer of a Single-Object encoded payload
    * @return the avro Schema
    */
  def retrieveSchemaAndAvroPayload(avroSingleObjectEncoded: ByteBuffer): Schema = {
    connector.retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
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
  def extractSchema(inputStream: InputStream): Either[Array[Byte], Schema] = {
    connector.extractSchema(inputStream, endianness, getSchema)
  }

  /**
    * Extracts the schema from the avro single-object encoded in the input array.
    *
    * @param array avro single-object encoded array
    * @return the schema ID extracted from the input data
    */
  def extractSchema(array: Array[Byte]): Either[Exception, Schema] = {
    connector.extractSchema(array, endianness, getSchema)
  }

  /**
    * Extracts a SchemaPayloadPair that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a SchemaPayloadPair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndPayload(avroSingleObjectEncoded: Array[Byte]): SchemaPayloadPair = {
    connector.retrieveSchemaAndPayload(avroSingleObjectEncoded, endianness, getSchema)
  }

  /**
    * Reloads all the schemas from the previously configured storage.
    */
  def reload(): AvroSchemaManager
}
