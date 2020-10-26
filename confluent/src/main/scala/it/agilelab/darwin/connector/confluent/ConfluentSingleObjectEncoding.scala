package it.agilelab.darwin.connector.confluent

import java.io.{ InputStream, OutputStream }
import java.nio.{ ByteBuffer, ByteOrder }
import java.util

import it.agilelab.darwin.common.DarwinConcurrentHashMap
import it.agilelab.darwin.manager.exception.DarwinException
import it.agilelab.darwin.manager.util.ByteArrayUtils._
import org.apache.avro.Schema

object ConfluentSingleObjectEncoding {
  private val V1_HEADER     = Array[Byte](0x00.toByte)
  private val ID_SIZE       = 4
  private val HEADER_LENGTH = V1_HEADER.length + ID_SIZE

  private val schemaMap = DarwinConcurrentHashMap.empty[Schema, Long]

  /**
    * Exception that can be thrown if the data is not single-object encoded
    */
  def parseException(): DarwinException = {
    new DarwinException(
      s"Byte array is not in correct format." +
        s" First ${V1_HEADER.length} bytes are not equal to ${byteArray2HexString(V1_HEADER)}"
    )
  }

  /**
    * Checks if a byte array is Avro Single-Object encoded (
    * <a href="https://avro.apache.org/docs/current/spec.html#single_object_encoding">Single-Object Encoding
    * Documentation</a>)
    *
    * @param data a byte array
    * @return true if the input byte array is Single-Object encoded
    */
  def isAvroSingleObjectEncoded(data: Array[Byte]): Boolean = {
    if (data.length < V1_HEADER.length) {
      throw new IllegalArgumentException(
        s"At least ${V1_HEADER.length} bytes " +
          s"required to store the Single-Object Encoder header"
      )
    }
    isAvroSingleObjectEncoded(ByteBuffer.wrap(data))
  }

  /**
    * Checks if a byte array is Avro Single-Object encoded (
    * <a href="https://avro.apache.org/docs/current/spec.html#single_object_encoding">Single-Object Encoding
    * Documentation</a>)
    *
    * @param data a ByteBuffer that will not be altered position wise by this method
    * @return true if the input byte array is Single-Object encoded
    */
  def isAvroSingleObjectEncoded(data: ByteBuffer): Boolean = {
    try {
      val originalPosition = data.position()
      val buffer           = new Array[Byte](V1_HEADER.length)
      data.get(buffer)
      data.position(originalPosition)
      util.Arrays.equals(buffer, V1_HEADER)
    } catch {
      case indexOutOfBoundsException: IndexOutOfBoundsException =>
        throw new IllegalArgumentException(
          s"At least ${V1_HEADER.length} bytes " +
            s"required to store the Single-Object Encoder header",
          indexOutOfBoundsException
        )
    }
  }

  /**
    * Create an array that creates a Single-Object encoded byte array.
    * By specifications the encoded array is obtained concatenating the V1_HEADER, the schema id and the avro-encoded
    * payload.
    *
    * @param avroPayload avro-serialized payload
    * @param schemaId    id of the schema used to encode the payload
    * @param endianness  a byte order to drive endianness of schemaId
    * @return a Single-Object encoded byte array
    */
  def generateAvroSingleObjectEncoded(
    avroPayload: Array[Byte],
    schemaId: Long,
    endianness: ByteOrder
  ): Array[Byte] = {
    Array.concat(V1_HEADER, schemaId.truncateIntToByteArray(endianness), avroPayload)
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header and returns the OutputStream
    *
    * @param endianness the endianness that will be used to persist fingerprint bytes, it won't affect how avro
    *                   payload is written, that is up to the darwin user
    * @return the input OutputStream
    */
  def writeHeaderToStream(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder): OutputStream = {
    byteStream.write(V1_HEADER)
    schemaId.writeToStream(byteStream, endianness)
    byteStream
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header then the avroValue and returns the OutputStream
    *
    * @param byteStream the stream to write to
    * @param avroValue  the value to be written to the stream
    * @param schemaId   id of the schema used to encode the payload
    * @param endianness the endianness that will be used to persist fingerprint bytes, it won't affect how avro
    *                   payload is written, that is up to the darwin user
    * @return the input OutputStream
    */
  def generateAvroSingleObjectEncoded(
    byteStream: OutputStream,
    avroValue: Array[Byte],
    schemaId: Long,
    endianness: ByteOrder
  ): OutputStream = {
    writeHeaderToStream(byteStream, schemaId, endianness)
    byteStream.write(avroValue)
    byteStream
  }

  /**
    * Writes to the given OutputStream the Single Object Encoding header then calls the avroWriter function to
    * possibly add data to the stream and finally returns the OutputStream
    *
    * @param byteStream the stream to write to
    * @param schemaId   id of the schema used to encode the payload
    * @param avroWriter function that will be called to add user generated avro to the stream
    * @param endianness the endianness that will be used to persist fingerprint bytes, it won't affect how avro
    *                   payload is written, that is up to the darwin user
    * @return the input OutputStream
    */
  def generateAvroSingleObjectEncoded(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder)(
    avroWriter: OutputStream => OutputStream
  ): OutputStream = {
    byteStream.write(V1_HEADER)
    schemaId.writeToStream(byteStream, endianness)
    avroWriter(byteStream)
  }

  /**
    * Extracts the schema ID from the avro single-object encoded byte array
    *
    * @param avroSingleObjectEncoded avro single-object encoded byte array
    * @param endianness              the endianness that will be used to read fingerprint bytes,
    *                                it won't affect how avro payload is read, that is up to the darwin user
    * @return the schema ID extracted from the input data
    */
  def extractId(avroSingleObjectEncoded: Array[Byte], endianness: ByteOrder): Long = {
    extractId(ByteBuffer.wrap(avroSingleObjectEncoded), endianness)
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
    if (avroSingleObjectEncoded.remaining() < HEADER_LENGTH) {
      throw new IllegalArgumentException(
        s"At least ${V1_HEADER.length} bytes " +
          s"required to store the Single-Object Encoder header"
      )
    } else {
      avroSingleObjectEncoded.position(avroSingleObjectEncoded.position() + V1_HEADER.length)
      readInt(avroSingleObjectEncoded, endianness)
    }
  }

  /**
    * Reads the content of the byte buffer honoring the input endianness and returns it.
    * When this method returns the buffer position will 8 bytes forward but the byte order will be unchanged whatever
    * the values of endianness and buf.order() are.
    */
  @inline
  def readInt(buf: ByteBuffer, endianness: ByteOrder): Long = {
    if (buf.order() == endianness) {
      buf.getInt
    } else {
      val lastEndianness = buf.order()
      buf.order(endianness)
      val toRet          = buf.getInt()
      buf.order(lastEndianness)
      toRet
    }
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
    val buffer              = new Array[Byte](HEADER_LENGTH)
    if (inputStream.markSupported()) {
      inputStream.mark(2)
    }
    val bytesReadMagicBytes = inputStream.read(buffer, 0, V1_HEADER.length)
    if (bytesReadMagicBytes == 2) {
      if (arrayEquals(buffer, V1_HEADER, 0, 0, 2)) {
        val bytesReadFingerPrint = inputStream.read(buffer, 2, ID_SIZE)
        if (bytesReadFingerPrint + bytesReadMagicBytes == HEADER_LENGTH) {
          val buf = ByteBuffer.wrap(buffer, 0, HEADER_LENGTH)
          // This cannot fail because the buffer length and start are already checked before and every 64 bits can
          // be interpreted as a Long value
          Right(extractId(buf, endianness))
        } else {
          Left(buffer.slice(0, bytesReadFingerPrint + bytesReadMagicBytes))
        }
      } else {
        if (inputStream.markSupported()) {
          inputStream.reset()
          inputStream.mark(0)
        }
        Left(buffer.slice(0, V1_HEADER.length))
      }
    } else {
      if (inputStream.markSupported()) {
        inputStream.reset()
        inputStream.mark(0)
      }
      Left(buffer.slice(0, bytesReadMagicBytes))
    }
  }

  /**
    * Extract the payload from an avro single-object encoded byte array, removing the header (the first 10 bytes)
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
    * @param schema     a Schema with unknown ID
    * @return the ID associated with the input schema
    */
  def getId(schema: Schema, fingerprinter: Schema => Long): Long = {
    schemaMap.getOrElseUpdate(schema, fingerprinter(schema))
  }

  /**
    * Converts a byte array into its hexadecimal string representation
    * e.g. for the V1_HEADER => [C3 01]
    *
    * @param bytes a byte array
    * @return the hexadecimal string representation of the input byte array
    */
  def byteArray2HexString(bytes: Array[Byte]): String = {
    bytes.map("%02X".format(_)).mkString("[", " ", "]")
  }
}
