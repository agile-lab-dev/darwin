package it.agilelab.darwin.server.rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.avro.Schema
import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, JsonParser, PrettyPrinter, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val printer: PrettyPrinter.type = PrettyPrinter

  implicit val schemaFormat: RootJsonFormat[Schema] = new RootJsonFormat[Schema] {

    override def write(obj: Schema): JsValue = JsonParser(obj.toString(true))

    override def read(json: JsValue): Schema = new Schema.Parser().parse(json.prettyPrint)
  }

  implicit val schemaWithIdFormat: RootJsonFormat[(Long, Schema)] = new RootJsonFormat[(Long, Schema)] {

    override def write(obj: (Long, Schema)): JsValue = JsObject(Map(
      "id" -> JsString(obj._1.toString),
      "schema" -> schemaFormat.write(obj._2)
    ))

    override def read(json: JsValue): (Long, Schema) = json match {
      case JsObject(fields) =>
        val id = fields.get("id") match {
          case Some(JsString(number)) => number
          case _ => throw new Exception("Id field should be a long")
        }

        val schema = fields.get("schema") match {
          case Some(x@JsObject(_)) => x
          case _ => throw new Exception("schema should be an object")
        }

        (id.toLong, schemaFormat.read(schema))
      case _ => throw new Exception("should be an object")
    }
  }
}
