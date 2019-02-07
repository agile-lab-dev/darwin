package it.agilelab.darwin.manager.util

import it.agilelab.darwin.manager.exception.DarwinException
import it.agilelab.darwin.manager.util.ByteArrayUtils._
import org.apache.avro.{Schema, SchemaNormalization}

object AvroSingleObjectEncodingUtils {
  private val V1_HEADER = Array[Byte](0xC3.toByte, 0x01.toByte)
  private val ID_SIZE = 8
  private val HEADER_LENGTH = V1_HEADER.length + ID_SIZE

  /** Exception that can be thrown if the data is not single-object encoded
    */
  lazy val parseException = new DarwinException(s"Byte array is not in correct format." +
    s" First ${V1_HEADER.length} bytes are not equal to $V1_HEADER")

  /** Checks if a byte array is Avro Single-Object encoded (
    * <a href="https://avro.apache.org/docs/current/spec.html#single_object_encoding">Single-Object Encoding
    * Documentation</a>)
    *
    * @param data a byte array
    * @return true if the input byte array is Single-Object encoded
    */
  def isAvroSingleObjectEncoded(data: Array[Byte]): Boolean = {
    if (data.length < V1_HEADER.length) throw new IllegalArgumentException(s"At least ${V1_HEADER.length} bytes " +
      s"required to store the Single-Object Encoder header")
    data.take(V1_HEADER.length).sameElements(V1_HEADER)
  }

  /** Create an array that creates a Single-Object encoded byte array.
    * By specifications the encoded array is obtained concatenating the V1_HEADER, the schema id and the avro-encoded
    * payload.
    *
    * @param avroPayload avro-serialized payload
    * @param schemaId    id of the schema used to encode the payload
    * @return a Single-Object encoded byte array
    */
  def generateAvroSingleObjectEncoded(avroPayload: Array[Byte], schemaId: Long): Array[Byte] = {
    Array.concat(V1_HEADER, schemaId.longToByteArray, avroPayload)
  }

  /** Extracts the schema ID from the avro single-object encoded byte array
    *
    * @param avroSingleObjectEncoded avro single-object encoded byte array
    * @return the schema ID extracted from the input data
    */
  def extractId(avroSingleObjectEncoded: Array[Byte]): Long = {
    avroSingleObjectEncoded.slice(V1_HEADER.length, HEADER_LENGTH).byteArrayToLong
  }

  /** Extract the payload from an avro single-object encoded byte array, removing the header (the first 10 bytes)
    *
    * @param avroSingleObjectEncoded avro single-object encoded byte array
    * @return the payload without the avro single-object encoded header
    */
  def dropHeader(avroSingleObjectEncoded: Array[Byte]): Array[Byte] = {
    avroSingleObjectEncoded.drop(HEADER_LENGTH)
  }

  /**
    * Extracts the ID from a Schema.
    *
    * @param schema a Schema with unknown ID
    * @return the ID associated with the input schema
    */
  def getId(schema: Schema): Long = SchemaNormalization.parsingFingerprint64(schema)
}
