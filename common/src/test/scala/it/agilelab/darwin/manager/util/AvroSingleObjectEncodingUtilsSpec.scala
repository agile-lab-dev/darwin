package it.agilelab.darwin.manager.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.{BufferUnderflowException, ByteBuffer, ByteOrder}
import java.util

import it.agilelab.darwin.manager.util.ByteArrayUtils._
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.util.ByteBufferInputStream

import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import it.agilelab.darwin.common.compat._

abstract class AvroSingleObjectEncodingUtilsSpec(val endianness: ByteOrder) extends AnyFlatSpec with Matchers {
  val sizeOfBuffer = 200
  val testId = 4560514203639509981L
  val parser = new Schema.Parser()


  val schema = parser.parse(
    """{
      |     "type": "record",
      |     "namespace": "com.example",
      |     "name": "FullName",
      |     "fields": [
      |       { "name": "first", "type": "string" },
      |       { "name": "last", "type": "string" }
      |     ]
      |}""".stripMargin)

  it should "compare two bytebuffers" in {
    val a1 = Array(1: Byte, 2: Byte, 3: Byte)
    val a2 = Array(1: Byte, 2: Byte, 3: Byte)
    (ByteBuffer.wrap(a1) == ByteBuffer.wrap(a2)) should be(true)
    (a1 == a2) should be(false)
    (a1 eq a2) should be(false)
  }

  "isAvroSingleObjectEncoded(Array)" should "return true if the array contains only the header" in {
    AvroSingleObjectEncodingUtils.
      isAvroSingleObjectEncoded(Array(0xC3.toByte, 0x01.toByte)) should be(true)
  }

  "isAvroSingleObjectEncoded(Buffer)" should "return true if the array contains the header plus random bytes" in {
    val buffer = new Array[Byte](sizeOfBuffer)
    Random.nextBytes(buffer)
    AvroSingleObjectEncodingUtils.
      isAvroSingleObjectEncoded(
        Array(0xC3.toByte, 0x01.toByte) ++ buffer) should be(true)
  }

  "extractId(InputStream)" should "return id = 77 and consume the stream" in {
    val stream = new ByteArrayInputStream(Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness))
    val id = AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    id should be(Right(testId))
    stream.read() should be(-1)
  }

  "extractId(InputStream)" should "return Left if the input stream is empty" in {
    val stream = new ByteArrayInputStream(Array.emptyByteArray)
    val id = AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    id.left.map(_.length == 0) should be(Left(true))
    stream.read() should be(-1)
  }

  "extractId(InputStream)" should "return Left if the input stream has only one byte" in {
    val stream = new ByteArrayInputStream(Array(Random.nextInt().toByte))
    val id = AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    id.left.map(_.length == 1) should be(Left(true))
    stream.read() should not be (-1)
    stream.read() should be(-1)
  }

  "extractId(InputStream)" should "return Left if the input stream does not have the expected header" in {
    val stream = new ByteArrayInputStream(Array(0xC3.toByte, 0x02.toByte))
    val id = AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    id.left.map(_.sameElements(Array(0xC3.toByte, 0x02.toByte))) should be(Left(true))
    stream.read().toByte should be(0xC3.toByte)
    stream.read().toByte should be(0x02.toByte)
    stream.read() should be(-1)
  }

  "writeHeaderToStream" should "write the header in the stream" in {
    val os = new ByteArrayOutputStream()
    AvroSingleObjectEncodingUtils.writeHeaderToStream(os, testId, endianness)
    os.toByteArray.sameElements(Array(testId.longToByteArray(endianness)))
  }

  "extractId(ByteBuffer)" should "return id = 77 and consume the buffer" in {
    val buffer = ByteBuffer.wrap(Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness))
    val id = AvroSingleObjectEncodingUtils.extractId(buffer, endianness)
    id should be(testId)
    buffer.hasRemaining should be(false)
  }

  "extractId(ByteBuffer)" should "throw an IllegalArgumentException" in {
    val buffer = ByteBuffer.wrap(Array.emptyByteArray)
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(buffer, endianness)
  }

  "extractId(ByteBuffer)" should "throw an IllegalArgumentException if the buffer has only one byte" in {
    val stream = ByteBuffer.wrap(Array(Random.nextInt().toByte))
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    stream.get()
    a[BufferUnderflowException] should be thrownBy stream.get()
  }

  "extractId(ByteBuffer)" should "throw an IllegalArgumentException " +
    "if the buffer does not have the expected header" in {

    val stream = ByteBuffer.wrap(Array(0xC3.toByte, 0x02.toByte))
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(stream, endianness)
    stream.get should be(0xC3.toByte)
    stream.get should be(0x02.toByte)
    a[BufferUnderflowException] should be thrownBy stream.get
  }


  "extractId(Array[Byte])" should "return id = 77 and consume the buffer" in {
    val buffer = Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness)
    val id = AvroSingleObjectEncodingUtils.extractId(buffer, endianness)
    id should be(testId)
  }

  "extractId(Array[Byte])" should "throw an IllegalArgumentException" in {
    val buffer = Array.emptyByteArray
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(buffer, endianness)
  }

  "extractId(Array[Byte])" should "throw an IllegalArgumentException if the buffer has only one byte" in {
    val stream = Array(Random.nextInt().toByte)
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(stream, endianness)
  }

  "extractId(Array[Byte])" should "throw an IllegalArgumentException " +
    "if the buffer does not have the expected header" in {

    val stream = Array(0xC3.toByte, 0x02.toByte)
    a[IllegalArgumentException] should be thrownBy AvroSingleObjectEncodingUtils.extractId(stream, endianness)
  }

  "generateAvroSingleObjectEncoded(Array[Byte])" should "generate a single object encoded array" in {
    val buffer = new Array[Byte](sizeOfBuffer)
    Random.nextBytes(buffer)

    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(buffer, testId, endianness) should contain theSameElementsAs (
      Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness) ++ buffer)
  }

  "generateAvroSingleObjectEncoded(OutputStream)" should "generate a single object encoded array" in {
    val random = new Array[Byte](sizeOfBuffer)
    Random.nextBytes(random)
    val bos = new ByteArrayOutputStream()
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(bos, random, testId, endianness)
      .asInstanceOf[ByteArrayOutputStream].toByteArray should contain theSameElementsAs (
      Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness) ++ random)
  }

  "generateAvroSingleObjectEncoded(OutputStream)2" should "generate a single object encoded array" in {
    val random = new Array[Byte](sizeOfBuffer)
    Random.nextBytes(random)
    val bos = new ByteArrayOutputStream()
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(bos, testId, endianness) { x =>
      x.write(random)
      x
    }.asInstanceOf[ByteArrayOutputStream].toByteArray should contain theSameElementsAs (
      Array(0xC3.toByte, 0x01.toByte) ++ testId.longToByteArray(endianness) ++ random)
  }

  "dropHeader" should "drop the first 10 bytes of the array" in {
    AvroSingleObjectEncodingUtils.dropHeader((0 until 100).map(_.toByte).toArray) should
      contain theSameElementsAs (10 until 100).map(_.toByte)
  }

  "getId" should "return the testId" in {

    AvroSingleObjectEncodingUtils.getId(schema) should be(testId)
  }


  "AvroSingleObjectEncodingUtils" should "encode using streams" in {
    val record = new GenericData.Record(schema)
    record.put("first", "Antonio")
    record.put("last", "Murgia")

    val stream = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(stream, null)
    val writer = new GenericDatumWriter[GenericRecord](schema)
    AvroSingleObjectEncodingUtils
      .generateAvroSingleObjectEncoded(stream, AvroSingleObjectEncodingUtils.getId(schema), endianness) {
        os =>
          writer.write(record, encoder)
          writer.write(record, encoder)
          os
      }
    encoder.flush()
    val iStream = new ByteArrayInputStream(stream.toByteArray)

    AvroSingleObjectEncodingUtils.extractId(iStream, endianness).rightMap { id =>
      id should be(testId)
      val decoder = DecoderFactory.get().binaryDecoder(iStream, null)
      val reader = new GenericDatumReader[GenericRecord](schema)
      val out = reader.read(null, decoder)
      val out2 = reader.read(null, decoder)
      out should be(record)
      out2 should be(record)
      true
    } should be(Right(true))
  }


  "AvroSingleObjectEncodingUtils" should "using byte buffers" in {
    val record = new GenericData.Record(schema)
    record.put("first", "Antonio")
    record.put("last", "Murgia")

    val stream = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get.binaryEncoder(stream, null)
    val writer = new GenericDatumWriter[GenericRecord](schema)
    AvroSingleObjectEncodingUtils.generateAvroSingleObjectEncoded(stream,
      AvroSingleObjectEncodingUtils.getId(schema), endianness) { os =>
      writer.write(record, encoder)
      writer.write(record, encoder)
      os
    }
    encoder.flush()
    val buffer = ByteBuffer.allocateDirect(stream.toByteArray.length)
    buffer.put(stream.toByteArray)
    buffer.rewind()

    val iStream = new ByteBufferInputStream(util.Arrays.asList(buffer))
    val id = AvroSingleObjectEncodingUtils.extractId(buffer, endianness)
    id should be(testId)
    val decoder = DecoderFactory.get().binaryDecoder(iStream, null)
    val reader = new GenericDatumReader[GenericRecord](schema)
    val out = reader.read(null, decoder)
    val out2 = reader.read(null, decoder)
    out should be(record)
    out2 should be(record)
  }
}
