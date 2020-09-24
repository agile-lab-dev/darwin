package it.agilelab.darwin.connector.confluent.rest

import java.io.InputStream
import org.apache.avro.Schema
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

trait JsonProtocol {

  val objectMapper = new ObjectMapper()

  def toSchema(in: InputStream): Schema = {
    val node = objectMapper.readTree(in)
    val schemaNode: JsonNode = node.get("schema")
    val parser = new Schema.Parser()
    parser.parse(schemaNode.asText())
  }
}
