package it.agilelab.darwin.connector.hbase

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.Connector
import org.apache.avro.reflect.ReflectData
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class HBaseConnectorSuite extends FlatSpec with Matchers with BeforeAndAfterAll {

  val connector: Connector = new HBaseConnectorCreator().create(ConfigFactory.load())

  "HBaseConnector" should "load all existing schemas" in {
    connector.fullLoad()
  }

  it should "insert and retrieve" in {
    val schemas = Seq(ReflectData.get().getSchema(classOf[HBaseMock]), ReflectData.get().getSchema(classOf[HBase2Mock]))
      .map(s => SchemaNormalization.parsingFingerprint64(s) -> s)
    connector.insert(schemas)
    val loaded: Seq[(Long, Schema)] = connector.fullLoad()
    assert(loaded.size == schemas.size)
    assert(loaded.forall(schemas.contains))
    val schema = connector.findSchema(loaded.head._1)
    assert(schema.isDefined)
    assert(schema.get == loaded.head._2)
    val noSchema = connector.findSchema(-1L)
    assert(noSchema.isEmpty)
  }

  "connector.tableCreationHint" should "print the correct hint for table creation" in {
    connector.tableCreationHint() should be(
      """To create namespace and table from an HBase shell issue:
        |  create_namespace 'AVRO'
        |  create 'AVRO:SCHEMA_REPOSITORY', '0'""".stripMargin)
  }

  "connector.tableExists" should "return true with existent table" in {
    connector.tableExists() should be(true)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    connector.createTable()
  }
}
