package it.agilelab.darwin.app.mock

import java.lang.reflect.Modifier

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.annotations.AvroSerde
import it.agilelab.darwin.app.mock.classes.{MyClass, MyNestedClass, OneField}
import it.agilelab.darwin.manager.AvroSchemaManager
import org.apache.avro.Schema
import org.apache.avro.reflect.ReflectData
import org.reflections.Reflections
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ApplicationSuite extends FlatSpec with Matchers {

  "AvroSchemaManager" should "not fail after the initialization" in {
    val schemas: Seq[Schema] = Seq(new SchemaGenerator[MyNestedClass].schema)
    assert(AvroSchemaManager.instance(ConfigFactory.empty).registerAll(schemas).size == 1)
  }

  it should "load all existing schemas and register a new one" in {
    val schemas: Seq[Schema] = Seq(new SchemaGenerator[MyNestedClass].schema)
    AvroSchemaManager.getSchema(0L)

    AvroSchemaManager.instance(ConfigFactory.empty).registerAll(schemas)

    val id = AvroSchemaManager.getId(schemas.head)
    assert(schemas.head == AvroSchemaManager.getSchema(id))
  }

  it should "get all previously registered schemas" in {
    val schema: Schema = new SchemaGenerator[MyNestedClass].schema
    val schema0 = AvroSchemaManager.getSchema(0L)
    val schema1 = AvroSchemaManager.getSchema(1L)
    assert(schema0 != schema1)
    assert(schema != schema0)
    assert(schema != schema1)
  }

  it should "generate all schemas for all the annotated classes with @AvroSerde" in {
    val reflections = new Reflections("it.agilelab.darwin.app.mock.classes")

    val oneFieldSchema = ReflectData.get().getSchema(classOf[OneField]).toString
    val myNestedSchema = ReflectData.get().getSchema(classOf[MyNestedClass]).toString
    val myClassSchema = ReflectData.get().getSchema(classOf[MyClass]).toString

    val annotationClass: Class[AvroSerde] = classOf[AvroSerde]
    val classes = reflections.getTypesAnnotatedWith(annotationClass).asScala.toSeq.filter(c => !c.isInterface && !Modifier.isAbstract(c.getModifiers))
    classes.foreach(println)
    val schemas = classes.map(c => ReflectData.get().getSchema(Class.forName(c.getName)).toString)
    schemas.foreach(println)
    Seq(oneFieldSchema, myClassSchema, myNestedSchema) should contain theSameElementsAs schemas
  }

}
