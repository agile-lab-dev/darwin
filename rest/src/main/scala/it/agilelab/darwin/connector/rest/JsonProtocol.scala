package it.agilelab.darwin.connector.rest

import java.io.InputStream

import org.apache.avro.Schema
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.JsonNodeFactory
import it.agilelab.darwin.common.compat._

trait JsonProtocol {
  val objectMapper = new ObjectMapper()

  def toJson(schemas: Seq[(Long, Schema)]): String = {

    val data = schemas.map { case (_, schema) =>
      objectMapper.readTree(schema.toString)
    }.foldLeft(JsonNodeFactory.instance.arrayNode()) { case (array, node) =>
      array.add(node)
      array
    }

    objectMapper.writeValueAsString(data)
  }

  def toSeqOfIdSchema(in: InputStream): Seq[(Long, Schema)] = {
    val node = objectMapper.readTree(in)

    node.getElements
      .toScala()
      .map { node =>
        val id         = node.get("id").asText().toLong
        val schemaNode = node.get("schema")

        val schemaToString = objectMapper.writeValueAsString(schemaNode)

        val parser = new Schema.Parser()

        val schema = parser.parse(schemaToString)

        (id, schema)
      }
      .toVector
  }

  def toSchema(in: InputStream): Schema = {
    val parser = new Schema.Parser()
    parser.parse(in)
  }
}
