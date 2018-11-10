package it.agilelab.darwin.manager
import com.typesafe.config.Config
import org.apache.avro.Schema

case class CachedLazyAvroSchemaManager(override val config: Config) extends CachedAvroSchemaManager {
  //TODO
  override def getId(schema: Schema): Long = ???

  override def getSchema(id: Long): Schema = ???
}
