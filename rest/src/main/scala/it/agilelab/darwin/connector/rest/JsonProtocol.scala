package it.agilelab.darwin.connector.rest

import java.io.InputStream

import it.agilelab.darwin.common.DarwinEntry
import org.apache.avro.Schema
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.JsonNodeFactory
import it.agilelab.darwin.common.compat._
import org.codehaus.jackson.JsonNode

trait JsonProtocol {
  val objectMapper = new ObjectMapper()

  def toJson(schemas: Seq[DarwinEntry]): String = {

    val data = schemas.map {
      case DarwinEntry(_, schema, version) =>
        val obj = JsonNodeFactory.instance.objectNode()
        obj.put("schema", objectMapper.readTree(schema.toString))
        obj.put("version", version)
        obj
    }.foldLeft(JsonNodeFactory.instance.arrayNode()) {
      case (array, node) =>
        array.add(node)
        array
    }

    objectMapper.writeValueAsString(data)
  }

  def toSeqOfIdSchema(in: InputStream): Seq[DarwinEntry] = {
    val node = objectMapper.readTree(in)
    node.getElements.toScala.map(jsonToEntry).toVector
  }

  def jsonToEntry(node: JsonNode): DarwinEntry = {
    val id = node.get("id").asText().toLong
    val schemaNode = node.get("schema")
    val version = node.get("version").asText().toLong
    val schemaToString = objectMapper.writeValueAsString(schemaNode)
    val parser = new Schema.Parser()
    val schema = parser.parse(schemaToString)
    DarwinEntry(id, schema, version)
  }

  def toDarwinEntry(in: InputStream): DarwinEntry = {
    jsonToEntry(objectMapper.readTree(in))
  }
}
