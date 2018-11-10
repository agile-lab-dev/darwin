package it.agilelab.darwin.manager

import java.lang

import com.typesafe.config.Config
import org.apache.avro.Schema

case class LazyAvroSchemaManager(override val config: Config) extends AvroSchemaManager {
  //TODO

  override def getId(schema: Schema): Long = ???

  override def getSchema(id: Long): Schema = ???

  override def registerAll(schemas: Seq[Schema]): Seq[(Long, Schema)] = ???

  override def registerAll(schemas: lang.Iterable[Schema]): lang.Iterable[IdSchemaPair] = ???

  override def reload(): AvroSchemaManager = ???
}
