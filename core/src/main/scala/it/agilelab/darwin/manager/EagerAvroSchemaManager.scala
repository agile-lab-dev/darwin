package it.agilelab.darwin.manager

import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.Config
import it.agilelab.darwin.common.Logging
import org.apache.avro.Schema

import scala.collection.JavaConverters._

/**
  * The main entry point to this library.
  * N.B.: each method all on this object must always be AFTER the initialization, performed invoking the initialize
  * method.
  */
case class EagerAvroSchemaManager(override val config: Config) extends AvroSchemaManager with Logging {

  private val _cache: AtomicReference[Option[AvroSchemaCache]] = new AtomicReference[Option[AvroSchemaCache]](None)
  def cache: AvroSchemaCache = _cache.get
    .getOrElse(throw new IllegalAccessException("Cache not loaded: accesses are allowed only if the cache has been " +
      "loaded"))

  initialize()

  private def initialize(): Unit = {
    log.debug("cache initialization...")
    _cache.compareAndSet(None, Some(AvroSchemaCacheFingerprint(connector.fullLoad())))
    log.debug("cache initialized")
  }

  /**
    * Reloads all the schemas from the previously configured storage.
    * Throws an exception if the cache wasn't already loaded (the getInstance method must always be used to
    * initialize the cache using the required configuration).
    */
  def reload(): AvroSchemaManager = {
    log.debug("reloading cache...")
    _cache.set(Some(AvroSchemaCacheFingerprint(connector.fullLoad())))
    log.debug("cache reloaded")
    this
  }

  override def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)] = {
    log.debug(s"registering ${schemas.size} schemas...")
    val (alreadyInCache, notInCache) = schemas.map(s => (cache.contains(s), s)).partition(_._1._1)
    val inserted = notInCache.map(e => e._1._2 -> e._2)
    connector.insert(inserted)
    val allSchemas = alreadyInCache.map(e => e._1._2 -> e._2) ++ inserted
    _cache.set(Some(cache.insert(inserted))) //TODO review
    log.debug(s"${allSchemas.size} schemas registered")
    allSchemas
  }

  override def registerAll(schemas: java.lang.Iterable[Schema]): java.lang.Iterable[IdSchemaPair] = {
    registerAll(schemas.asScala.toSeq).map { case (id, schema) => IdSchemaPair.create(id, schema) }.asJava
  }

  override def getId(schema: Schema): Long = cache.getId(schema)

  override def getSchema(id: Long): Schema = cache.getSchema(id)
}