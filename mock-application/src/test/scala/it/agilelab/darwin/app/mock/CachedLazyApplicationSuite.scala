package it.agilelab.darwin.app.mock

import com.typesafe.config.{ Config, ConfigFactory }
import it.agilelab.darwin.annotations.AvroSerde
import it.agilelab.darwin.app.mock.classes.{ MyClass, MyNestedClass, NewClass, OneField }
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{ Connector, ConnectorFactory, SchemaReader }
import it.agilelab.darwin.manager.{ AvroSchemaManager, CachedLazyAvroSchemaManager }
import org.apache.avro.reflect.ReflectData
import org.apache.avro.{ Schema, SchemaNormalization }
import org.reflections.Reflections
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.lang.reflect.Modifier
import java.nio.ByteOrder

class BigEndianCachedLazyApplicationSuite extends CachedLazyApplicationSuite(ByteOrder.BIG_ENDIAN)

class LittleEndianCachedLazyApplicationSuite extends CachedLazyApplicationSuite(ByteOrder.LITTLE_ENDIAN)

abstract class CachedLazyApplicationSuite(val endianness: ByteOrder) extends AnyFlatSpec with Matchers {

  private val mockClassAloneFingerprint  = 6675579114512671233L
  private val mockClassParentFingerprint = -6310800772237892477L
  val config: Config                     = ConfigFactory.load()
  val connector: Connector               = ConnectorFactory.connector(config)
  val manager: AvroSchemaManager         = new CachedLazyAvroSchemaManager(connector, endianness)

  "CachedLazyAvroSchemaManager" should "not fail after the initialization" in {
    val schemas: Seq[Schema] = Seq(SchemaReader.readFromResources("MyNestedClass.avsc"))
    assert(manager.registerAll(schemas).size == 1)
  }

  it should "load all existing schemas and register a new one" in {
    val schemas: Seq[Schema] = Seq(SchemaReader.readFromResources("MyNestedClass.avsc"))
    manager.getSchema(0L)

    manager.registerAll(schemas)

    val id = manager.getId(schemas.head)
    assert(manager.getSchema(id).isDefined)
    assert(schemas.head == manager.getSchema(id).get)
  }

  it should "get all previously registered schemas" in {
    val schema: Schema = SchemaReader.readFromResources("MyNestedClass.avsc")
    val schema0        = manager.getSchema(mockClassAloneFingerprint)
    val schema1        = manager.getSchema(mockClassParentFingerprint)
    assert(schema0.isDefined)
    assert(schema1.isDefined)
    assert(schema0.get != schema1.get)
    assert(schema != schema0.get)
    assert(schema != schema1.get)
  }

  it should "generate all schemas for all the annotated classes with @AvroSerde" in {
    val reflections = new Reflections("it.agilelab.darwin.app.mock.classes")

    val oneFieldSchema = ReflectData.get().getSchema(classOf[OneField]).toString
    val myNestedSchema = ReflectData.get().getSchema(classOf[MyNestedClass]).toString
    val myClassSchema  = ReflectData.get().getSchema(classOf[MyClass]).toString

    val annotationClass: Class[AvroSerde] = classOf[AvroSerde]
    val classes                           = reflections
      .getTypesAnnotatedWith(annotationClass)
      .toScala()
      .toSeq
      .filter(c => !c.isInterface && !Modifier.isAbstract(c.getModifiers))
    val schemas                           = classes.map(c => ReflectData.get().getSchema(Class.forName(c.getName)).toString)
    Seq(oneFieldSchema, myClassSchema, myNestedSchema) should contain theSameElementsAs schemas
  }

  it should "reload all schemas from the connector" in {
    val newSchema = ReflectData.get().getSchema(classOf[NewClass])
    val newId     = SchemaNormalization.parsingFingerprint64(newSchema)
    assert(manager.getSchema(newId).isEmpty)

    connector.insert(Seq(newId -> newSchema))
    assert(manager.getSchema(newId).isDefined)
    assert(manager.getSchema(newId).get == newSchema)
  }

  it should "not call getId when retrieving a schema out of the cache" in {
    val oneFieldSchema = ReflectData.get().getSchema(classOf[OneField])
    var calls          = 0
    val manager        = new CachedLazyAvroSchemaManager(
      new Connector {
        override def createTable(): Unit                                              = ()
        override def tableExists(): Boolean                                           = true
        override def tableCreationHint(): String                                      = ""
        override def fullLoad(): Seq[(Long, Schema)]                                  = Seq.empty
        override def insert(schemas: Seq[(Long, Schema)]): Unit                       = ()
        override def findSchema(id: Long): Option[Schema]                             = Some(oneFieldSchema)
        override def retrieveLatestSchema(identifier: String): Option[(Long, Schema)] = Some(1L -> oneFieldSchema)
      },
      endianness
    ) {
      override def getId(schema: Schema): Long = {
        calls += 1
        super.getId(schema)
      }
    }
    manager.getSchema(3L) shouldNot be(null) // scalastyle:ignore
    calls shouldBe 0
  }
}
