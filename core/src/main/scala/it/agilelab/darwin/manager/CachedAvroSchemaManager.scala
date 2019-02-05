package it.agilelab.darwin.manager

import java.util.concurrent.atomic.AtomicReference

import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema

/**
  * Implementation of AvroSchemaManager that defines a cache where the storage data is loaded, in order to reduce the
  * number of accesses to the storage.
  */
abstract class CachedAvroSchemaManager(connector: Connector) extends AvroSchemaManager(connector) {
  protected val _cache: AtomicReference[Option[AvroSchemaCache]] = new AtomicReference[Option[AvroSchemaCache]](None)

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
  override def reload(): AvroSchemaManager = {
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


  /**
    * Retrieves all registered schemas
    *
    * @return A Sequence of (ID, Schema)
    */
  override def getAll: Seq[(Long, Schema)] = cache.getAll
}
