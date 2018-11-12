package it.agilelab.darwin.manager
import com.typesafe.config.Config
import org.apache.avro.Schema

case class CachedLazyAvroSchemaManager(override val config: Config) extends CachedAvroSchemaManager {

  override def getSchema(id: Long): Option[Schema] = {
    cache.getSchema(id).orElse{
      val schema: Option[Schema] = connector.findSchema(id)
      schema.foreach(s => _cache.set(Some(cache.insert(Seq(getId(s) -> s)))))
      schema
    }
  }
}
