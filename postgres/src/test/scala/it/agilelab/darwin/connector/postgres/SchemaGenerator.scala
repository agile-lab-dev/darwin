package it.agilelab.darwin.connector.postgres

import com.sksamuel.avro4s._
import org.apache.avro.Schema

final class SchemaGenerator[T]()(implicit schemaFor: SchemaFor[T],
                                 fromRecord: FromRecord[T],
                                 toRecord: ToRecord[T]) extends Serializable {
  def schema: Schema = schemaFor.apply()
}
