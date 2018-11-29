package it.agilelab.darwin.manager

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, ConnectorFactory, Logging}
import it.agilelab.darwin.manager.exception.ConnectorNotFoundException
import it.agilelab.darwin.manager.util.ConfigurationKeys
import jdk.nashorn.internal.runtime.ParserException
import org.apache.avro.Schema

import scala.collection.JavaConverters._

object AvroSchemaManager extends Logging {

  private val V1_HEADER = Array[Byte](0xC3.toByte, 0x01.toByte)
  private val ID_SIZE = 8
  private val HEADER_LENGTH = V1_HEADER.length + ID_SIZE

  private var _instance: AvroSchemaManager = _
  private val _cache: AtomicReference[Option[AvroSchemaCache]] = new AtomicReference[Option[AvroSchemaCache]](None)

  /**
    * Returns an instance of AvroSchemaManager that can be used to register schemas.
    *
    * @param config the Config that is passed to the connector
    * @return an instance of AvroSchemaManager
    */
  @throws[ConnectorNotFoundException]
  def getInstance(config: Config): AvroSchemaManager = {
    synchronized {
      if (_instance == null) {
        log.debug("creating instance of AvroSchemaManager")
        _instance = AvroSchemaManager(config)
        log.debug("AvroSchemaManager instance created")
      }
    }
    _instance
  }

  /**
    * Reloads all the schemas from the previously configured storage.
    * Throws an exception if the cache wasn't already loaded (the getInstance method must always be used to
    * initialize the cache using the required configuration).
    */
  def reload(): Unit = {
    _instance.reload()
  }

  def cache: AvroSchemaCache = _cache.get
    .getOrElse(throw new IllegalAccessException("Cache not loaded: accesses are allowed only if the cache has been " +
      "loaded"))

  /**
    * Extracts the ID from a Schema.
    *
    * @param schema a Schema with unknown ID
    * @return the ID associated with the input schema
    */
  def getId(schema: Schema): Long = cache.getId(schema)

  /**
    * Extracts the Schema from its ID.
    *
    * @param id a Long representing an ID
    * @return the Schema associated to the input ID
    */
  def getSchema(id: Long): Schema = cache.getSchema(id)


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

  /** Create an array that creates a Single-Object encoded byte array.
    * By specifications the encoded array is obtained concatenating the V1_HEADER, the schema id and the avro-encoded
    * payload.
    *
    * @param avroPayload avro-serialized payload
    * @param schema      the schema used to encode the payload
    * @return a Single-Object encoded byte array
    */
  def generateAvroSingleObjectEncoded(avroPayload: Array[Byte], schema: Schema): Array[Byte] = {
    generateAvroSingleObjectEncoded(avroPayload, getId(schema))
  }

  /** Extracts a Tuple2 that contains the Schema and the Avro-encoded payload
    *
    * @param avroSingleObjectEncoded a byte array of a Single-Object encoded payload
    * @return a pair containing the Schema and the payload of the input array
    */
  def retrieveSchemaAndAvroPayload(avroSingleObjectEncoded: Array[Byte]): (Schema, Array[Byte]) = {
    if (isAvroSingleObjectEncoded(avroSingleObjectEncoded)) {
      getSchema(avroSingleObjectEncoded.slice(V1_HEADER.length, HEADER_LENGTH).byteArrayToLong) ->
        avroSingleObjectEncoded.drop(HEADER_LENGTH)
    }
    else {
      throw new ParserException(s"Byte array is not in correct format. First ${V1_HEADER.length} bytes are not equal" +
        s" to $V1_HEADER")
    }
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

  implicit class EnrichedLong(l: Long) {

    /** Converts Long to Array[Byte].
      */
    def longToByteArray: Array[Byte] = {
      (0 to 7).foldLeft(Array.empty[Byte])((z, idx) => z :+ ((l >> ((7 - idx) * 8)) & 0xff).toByte)
    }
  }

  implicit class EnrichedByteArray(a: Array[Byte]) {

    /** Converts Array[Byte] to Long.
      * throws java.nio.BufferUnderflowException if array size isn't 8. (Long require 64 bit)
      */
    def byteArrayToLong: Long = ByteBuffer.wrap(a).getLong
  }

}

/**
  * The main entry point to this library.
  * N.B.: each method all on this object must always be AFTER the initialization, performed invoking the initialize
  * method.
  */
case class AvroSchemaManager(config: Config) extends Logging {

  private[darwin] val connector: Connector =
    ConnectorFactory.creators().headOption.map(_.create(config))
      .getOrElse(throw new ConnectorNotFoundException(config))

  private def initialize(): Unit = {
    if (config.getBoolean(ConfigurationKeys.CREATE_TABLE)) {
      connector.createTable()
    } else if (!connector.tableExists()) {
      log.warn(s"Darwin table does not exists and has not been created (${ConfigurationKeys.CREATE_TABLE} was false)")
      log.warn(connector.tableCreationHint())
    }
    log.debug("cache initialization...")
    AvroSchemaManager._cache.compareAndSet(None, Some(AvroSchemaCacheFingerprint(connector.fullLoad())))
    log.debug("cache initialized")
  }

  private def reload(): Unit = {
    log.debug("reloading cache...")
    AvroSchemaManager._cache.set(Some(AvroSchemaCacheFingerprint(connector.fullLoad())))
    log.debug("cache reloaded")
  }

  initialize()

  /**
    * Checks if all the input Schema elements are already in the cache. Then, it performs an insert on the
    * storage for all the elements not found on the cache, and then returns each input schema paired with its ID.
    *
    * @param schemas all the Schema that should be registered
    * @return a sequence of pairs of the input schemas associated with their IDs
    */
  def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)] = {
    log.debug(s"registering ${schemas.size} schemas...")
    val (alreadyInCache, notInCache) = schemas.map(s => (AvroSchemaManager.cache.contains(s), s)).partition(_._1._1)
    val inserted = notInCache.map(e => e._1._2 -> e._2)
    connector.insert(inserted)
    val allSchemas = alreadyInCache.map(e => e._1._2 -> e._2) ++ inserted
    AvroSchemaManager._cache.set(Some(AvroSchemaManager.cache.insert(inserted))) //TODO review
    log.debug(s"${allSchemas.size} schemas registered")
    allSchemas
  }

  /**
    *
    * JAVA API: Checks if all the input Schema elements are already in the cache. Then, it performs an insert on the
    * storage for all the elements not found on the cache, and then returns each input schema paired with its ID.
    *
    * @param schemas all the Schema that should be registered
    * @return a sequence of pairs of the input schemas associated with their IDs
    */
  def registerAll(schemas: java.lang.Iterable[Schema]): java.lang.Iterable[IdSchemaPair] = {
    registerAll(schemas.asScala.toSeq).map { case (id, schema) => IdSchemaPair.create(id, schema) }.asJava
  }
}
