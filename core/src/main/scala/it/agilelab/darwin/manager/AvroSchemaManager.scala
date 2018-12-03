package it.agilelab.darwin.manager

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorFactory, Logging}
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException
import it.agilelab.darwin.manager.util.{AvroSingleObjectEncodingUtils, ConfigurationKeys}
import org.apache.avro.{Schema, SchemaNormalization}

import scala.collection.JavaConverters._

/**
  * The main entry point of the Darwin library.
  * An instance of AvroSchemaManager should ALWAYS be obtained through the AvroSchemaManagerFactory.
  * The manager is responsible for schemas registration, retrieval and updates.
  */
trait AvroSchemaManager extends Logging {

  protected def config: Config

  protected[darwin] lazy val connector: Connector = {
    val cnt = ConnectorFactory.creators().headOption.map(_.create(config))
      .getOrElse(throw new ConnectorNotFoundException(config))

    if (config.getBoolean(ConfigurationKeys.CREATE_TABLE)) {
      cnt.createTable()
    } else if (!cnt.tableExists()) {
      log.warn(s"Darwin table does not exists and has not been created (${ConfigurationKeys.CREATE_TABLE} was false)")
      log.warn(cnt.tableCreationHint())
    }
    cnt
  }

  /**
    * Extracts the ID from a Schema.
    *
    * @param schema a Schema with unknown ID
    * @return the ID associated with the input schema
    */
  def getId(schema: Schema): Long = SchemaNormalization.parsingFingerprint64(schema)

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
    registerAll(schemas.asScala.toSeq).map { case (id, schema) => IdSchemaPair.create(id, schema) }.asJava
  }

  /** Create an array that creates a Single-Object encoded byte array.
    * By specifications the encoded array is obtained concatenating the V1_HEADER, the schema id and the avro-encoded
    * payload.
    *
    * @param avroPayload avro-serialized payload
    * @param schema      the schema used to encode the payload
    * @return a Single-Object encoded byte array
    */
  def generateAvroSingleObjectEncoded(avroPayload: Array[Byte], schema: Schema): Array[Byte] = {
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(avroPayload, getId(schema))
  }

  /** Extracts a Tuple2 that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a pair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndAvroPayload(avroSingleObjectEncoded: Array[Byte]): (Schema, Array[Byte]) = {
    if (AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      getSchema(AvroSingleObjectEncodingUtils.extractId(avroSingleObjectEncoded)).get ->
        AvroSingleObjectEncodingUtils.dropHeader(avroSingleObjectEncoded)
    }
    else { throw AvroSingleObjectEncodingUtils.parseException }
  }

  /** Extracts a [[SchemaPayloadPair]] that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a [[SchemaPayloadPair]] containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndPayload(avroSingleObjectEncoded: Array[Byte]): SchemaPayloadPair = {
    val (schema, payload) = retrieveSchemaAndAvroPayload(avroSingleObjectEncoded)
    SchemaPayloadPair.create(schema, payload)
  }

  /**
    * Reloads all the schemas from the previously configured storage.
    */
  def reload(): AvroSchemaManager
}
