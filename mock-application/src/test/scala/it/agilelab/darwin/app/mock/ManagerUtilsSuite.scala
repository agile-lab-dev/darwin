package it.agilelab.darwin.app.mock

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.app.mock.classes.OneField
import it.agilelab.darwin.manager.AvroSchemaManagerFactory
import it.agilelab.darwin.manager.util.AvroSingleObjectEncodingUtils
import it.agilelab.darwin.manager.util.ByteArrayUtils._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random
import scala.collection.JavaConverters._

class ManagerUtilsSuite extends FlatSpec with Matchers {

  "AvroSchemaManager utilities" should "create a Single-Object encoded byte array" in {
    val originalSchema = new SchemaGenerator[OneField].schema
    val config = ConfigFactory.parseMap(Map("type" -> "cached_eager").asJava)
    val manager = AvroSchemaManagerFactory.initialize(config)
    manager.registerAll(Seq(originalSchema))
    val originalPayload = new Array[Byte](10)
    Random.nextBytes(originalPayload)
    val data: Array[Byte] = manager.generateAvroSingleObjectEncoded(originalPayload, originalSchema)
    assert(AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(data))
    val (schema, payload) = manager.retrieveSchemaAndAvroPayload(data)
    assert(schema == originalSchema)
    assert(originalPayload sameElements payload)
  }

  it should "convert a long to byte array and back" in {
    val longs = (1 to 10).map(_ => Random.nextLong())
    assert(longs == longs.map(_.longToByteArray.byteArrayToLong))
  }

}
