package it.agilelab.darwin.connector.mysql

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.Connector
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.{FlatSpec, Matchers}
import org.apache.avro.reflect.ReflectData


class MySQLConnectorSuite extends FlatSpec with Matchers {
  val connector: Connector = new MySQLConnectorCreator().create(ConfigFactory.load("mysql.properties"))

  "MySQLConnector" should "load all existing schemas" in {
    connector.fullLoad()
  }

  it should "insert and retrieve" in {
    val outerSchema = new Schema.Parser().parse(getClass.getClassLoader.getResourceAsStream("mysqlmock.avsc"))
    val innerSchema = outerSchema.getField("four").schema()

    val schemas = Seq(innerSchema, outerSchema)
      .map(s => SchemaNormalization.parsingFingerprint64(s) -> s)
    connector.insert(schemas)
    val loaded: Seq[(Long, Schema)] = connector.fullLoad()
    assert(loaded.size == schemas.size)
    assert(loaded.forall(schemas.contains))
  }

  it should "check schemas" in {
    val outerSchema: Schema = ReflectData.get().getSchema(classOf[MySQLMock])
    val innerSchema: Schema = ReflectData.get().getSchema(classOf[MySQL2Mock])

    val loaded: Seq[Schema] = connector.fullLoad().map(s => s._2)

    assert(loaded.contains(outerSchema))
    assert(loaded.contains(innerSchema))
  }
}
