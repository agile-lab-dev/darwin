package it.agilelab.darwin.common

import org.apache.avro.Schema

case class DarwinEntry(fingerprint: Long,
                       schema: Schema,
                       version: Long) {
  val schemaFullName: String = {
    try {
      schema.getNamespace + schema.getName
    } catch {
      case _: Throwable => schema.getName
    }
  }
}

case class DarwinEntryNoVersion(fingerprint: Long,
                                schema: Schema) {
  def withVersion(v: Long): DarwinEntry = DarwinEntry(fingerprint, schema, v)
}