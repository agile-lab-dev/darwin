package it.agilelab.darwin.app.mock

import java.nio.{ByteBuffer, ByteOrder}

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.SchemaReader
import it.agilelab.darwin.manager.AvroSchemaManagerFactory
import it.agilelab.darwin.manager.util.{AvroSingleObjectEncodingUtils, ConfigurationKeys}
import it.agilelab.darwin.manager.util.ByteArrayUtils._

import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BigEndianManagerUtilsSuite extends ManagerUtilsSuite(ByteOrder.BIG_ENDIAN)

class LittleEndianManagerUtilsSuite extends ManagerUtilsSuite(ByteOrder.LITTLE_ENDIAN)

abstract class ManagerUtilsSuite(endianness: ByteOrder) extends AnyFlatSpec with Matchers {

  "AvroSchemaManager utilities" should "create a Single-Object encoded byte array" in {
    val ORIGINAL_LENGTH: Int = 10
    val originalSchema = SchemaReader.readFromResources("OneField.avsc")
    val config =
      ConfigFactory
        .parseMap(new java.util.HashMap[String, String]() {
          {
            put(ConfigurationKeys.MANAGER_TYPE, ConfigurationKeys.CACHED_EAGER)
            put(ConfigurationKeys.ENDIANNESS, endianness.toString)
          }
        })
        .withFallback(ConfigFactory.load())
        .resolve()
    val manager = AvroSchemaManagerFactory.initialize(config)
    manager.registerAll(Seq(originalSchema))
    val originalPayload = new Array[Byte](ORIGINAL_LENGTH)
    Random.nextBytes(originalPayload)
    val data: Array[Byte] = manager.generateAvroSingleObjectEncoded(originalPayload, originalSchema)
    assert(AvroSingleObjectEncodingUtils.isAvroSingleObjectEncoded(data))
    val (schema, payload) = manager.retrieveSchemaAndAvroPayload(data)
    assert(schema == originalSchema)
    assert(originalPayload sameElements payload)
  }

  it should "convert a long to byte array and back" in {
    val longs = (1 to 10).map(_ => Random.nextLong())

    assert(
      longs == longs.map(
        x =>
          AvroSingleObjectEncodingUtils
            .readLong(ByteBuffer.wrap(x.longToByteArray(endianness)), endianness)
      )
    )
  }

}
