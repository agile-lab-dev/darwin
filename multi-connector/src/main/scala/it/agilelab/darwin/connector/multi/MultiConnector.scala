package it.agilelab.darwin.connector.multi

import it.agilelab.darwin.common.Connector
import it.agilelab.darwin.common.compat.RightBiasedEither
import it.agilelab.darwin.manager.SchemaPayloadPair
import it.agilelab.darwin.manager.exception.DarwinException
import it.agilelab.darwin.manager.util.{ AvroSingleObjectEncodingUtils, ConfluentSingleObjectEncoding }
import org.apache.avro.Schema

import java.io.{ ByteArrayInputStream, InputStream, OutputStream, SequenceInputStream }
import java.nio.{ ByteBuffer, ByteOrder }

class MultiConnector(
  val registrator: Connector,
  val confluentConnectors: Option[Connector],
  val singleObjectEncodingConnectors: List[Connector]
) extends Connector {

  override def createTable(): Unit = registrator.createTable()

  /**
    * Returns whether or not the configured table exists
    */
  override def tableExists(): Boolean = registrator.tableExists()

  /**
    * @return a message indicating the user what he/she should do to create the table him/herself
    */
  override def tableCreationHint(): String = registrator.tableCreationHint()

  /**
    * Loads all schemas found on the storage.
    * This method can be invoked multiple times: to initialize the initial values or to update the existing ones with
    * the new data found on the storage.
    *
    * @return a sequence of all the pairs (ID, schema) found on the storage
    */
  override def fullLoad(): Seq[(Long, Schema)] =
    (confluentConnectors.toSeq.flatMap(_.fullLoad()) ++ singleObjectEncodingConnectors.flatMap(_.fullLoad())).distinct

  /**
    * Inserts all the schema passed as parameters in the storage.
    * This method is called when new schemas should be registered in the storage (the test if a schema is already in
    * the storage should be performed before the invocation of this method, e.g. by checking them against the
    * pre-loaded cache).
    *
    * @param schemas a sequence of pairs (ID, schema) Schema entities to insert in the storage.
    */
  override def insert(schemas: Seq[(Long, Schema)]): Unit = registrator.insert(schemas)

  /**
    * Retrieves a single schema using its ID from the storage.
    *
    * @param id the ID of the schema
    * @return an option that is empty if no schema was found for the ID or defined if a schema was found
    */
  override def findSchema(id: Long): Option[Schema] =
    headOfIterator(
      (confluentConnectors.iterator ++ singleObjectEncodingConnectors.iterator)
        .flatMap(_.findSchema(id))
    )

  private def headOfIterator[A](it: Iterator[A]): Option[A] =
    if (it.hasNext) {
      Some(it.next())
    } else {
      None
    }

  /**
    * Retrieves the latest schema for a given string identifier (not to be confused with the fingerprint id).
    * This API might not be implemented by all connectors, which should return None
    */
  override def retrieveLatestSchema(identifier: String): Option[(Long, Schema)] =
    headOfIterator(confluentConnectors.iterator.flatMap(_.retrieveLatestSchema(identifier)))

  /**
    * Generate a fingerprint for a schema, the default implementation is SchemaNormalization.parsingFingerprint64
    *
    * @param schema the schema to fingerprint
    * @return the schema id
    */
  override def fingerprint(schema: Schema): Long = registrator.fingerprint(schema)

  /**
    * Writes to the given OutputStream the Single Object Encoding header and returns the OutputStream
    *
    * @return the input OutputStream
    */
  override def writeHeaderToStream(byteStream: OutputStream, schemaId: Long, endianness: ByteOrder): OutputStream =
    registrator.writeHeaderToStream(byteStream, schemaId, endianness)

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
  ): Array[Byte] = registrator.generateAvroSingleObjectEncoded(avroPayload, schema, endianness, getId)

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
  ): OutputStream = registrator.generateAvroSingleObjectEncoded(byteStream, avroValue, schemaId, endianness)

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
  ): OutputStream = registrator.generateAvroSingleObjectEncoded(byteStream, schemaId, endianness)(avroWriter)

  sealed private trait SingleObjectEncoded
  private case object ConfluentSingleObjectEncoded extends SingleObjectEncoded
  private case object AvroSingleObjectEncoded      extends SingleObjectEncoded
  private case object NoSingleObjectEncoded        extends SingleObjectEncoded

  private def whichEncoding(byte: Array[Byte]): SingleObjectEncoded = {
    if (ConfluentSingleObjectEncoding.isAvroSingleObjectEncoded(byte)) {
      ConfluentSingleObjectEncoded
    } else if (AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(byte)) {
      AvroSingleObjectEncoded
    } else {
      NoSingleObjectEncoded
    }
  }

  private def whichEncoding(byte: ByteBuffer): SingleObjectEncoded = {
    if (ConfluentSingleObjectEncoding.isAvroSingleObjectEncoded(byte)) {
      ConfluentSingleObjectEncoded
    } else if (AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(byte)) {
      AvroSingleObjectEncoded
    } else {
      NoSingleObjectEncoded
    }
  }

  private def connectorInstance(enc: SingleObjectEncoded) = {
    enc match {
      case ConfluentSingleObjectEncoded =>
        confluentConnectors.getOrElse(
          throw new DarwinException("Data is confluent encoded but no confluent connectors are configured")
        )
      case AvroSingleObjectEncoded      =>
        singleObjectEncodingConnectors.headOption.getOrElse(
          throw new DarwinException(
            "Data is avro single object encoded but no avro single object encoding connectors are configured"
          )
        )
      case NoSingleObjectEncoded        => throw new DarwinException("Data is not in single object encoding")
    }
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
    val c = connectorInstance(whichEncoding(avroSingleObjectEncoded))
    c.retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
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
    val c = connectorInstance(whichEncoding(avroSingleObjectEncoded))
    c.retrieveSchemaAndAvroPayload(avroSingleObjectEncoded, endianness, getSchema)
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
  override def extractId(inputStream: InputStream, endianness: ByteOrder): Either[Array[Byte], Long] = {
    ConfluentSingleObjectEncoding.extractId(inputStream, endianness).left.flatMap { readBytes =>
      if (readBytes.isEmpty) {
        AvroSingleObjectEncodingUtils.extractId(inputStream, endianness)
      } else {
        AvroSingleObjectEncodingUtils.extractId(
          new SequenceInputStream(new ByteArrayInputStream(readBytes), inputStream),
          endianness
        )
      }
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
    ConfluentSingleObjectEncoding
      .extractId(inputStream, endianness)
      .rightMap(id => id -> (ConfluentSingleObjectEncoded: SingleObjectEncoded))
      .left
      .flatMap { readBytes =>
        if (readBytes.isEmpty) {
          AvroSingleObjectEncodingUtils
            .extractId(inputStream, endianness)
            .rightMap(_ -> AvroSingleObjectEncoded)
        } else {
          AvroSingleObjectEncodingUtils
            .extractId(
              new SequenceInputStream(new ByteArrayInputStream(readBytes), inputStream),
              endianness
            )
            .rightMap(_ -> AvroSingleObjectEncoded)
        }
      }
      .rightFlatMap { case (id, enc) => connectorInstance(enc).findSchema(id).toRight(Array.emptyByteArray) }
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
    val c = connectorInstance(whichEncoding(array))
    c.extractSchema(array, endianness, getSchema)
  }

  /**
    * Extracts the schema ID from the avro single-object encoded byte array
    *
    * @param array      avro single-object encoded byte array
    * @param endianness the endianness that will be used to read fingerprint bytes,
    *                   it won't affect how avro payload is read, that is up to the darwin user
    * @return the schema ID extracted from the input data
    */
  override def extractId(array: Array[Byte], endianness: ByteOrder): Long = {
    val c = connectorInstance(whichEncoding(array))
    c.extractId(array, endianness)
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
  override def extractId(avroSingleObjectEncoded: ByteBuffer, endianness: ByteOrder): Long = {
    val c = connectorInstance(whichEncoding(avroSingleObjectEncoded))
    c.extractId(avroSingleObjectEncoded, endianness)
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
    val c = connectorInstance(whichEncoding(avroSingleObjectEncoded))
    c.retrieveSchemaAndPayload(avroSingleObjectEncoded, endianness, getSchema)
  }
}
