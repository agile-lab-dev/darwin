package it.agilelab.darwin.connector.hbase

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.Connector
import org.apache.avro.{Schema, SchemaNormalization}
import org.apache.avro.reflect.ReflectData
import org.scalatest.{FlatSpec, Matchers}

class HBaseConnectorSuite extends FlatSpec with Matchers {

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

}
