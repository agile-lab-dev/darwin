package it.agilelab.darwin.connector.hbase

import java.nio.file.Files

import com.typesafe.config.{ ConfigFactory, ConfigValueFactory}
import it.agilelab.darwin.common.Connector
import org.apache.avro.reflect.ReflectData
import org.apache.avro.{Schema, SchemaNormalization}
import org.apache.hadoop.hbase.{HBaseTestingUtility, MiniHBaseCluster}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HBaseConnectorSuite extends AnyFlatSpec with Matchers with BeforeAndAfterAll {


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


  var connector: Connector = _

  var util : HBaseTestingUtility = _
  var minicluster: MiniHBaseCluster = _

  override def beforeAll(): Unit = {
    util = new HBaseTestingUtility()
    minicluster = util.startMiniCluster()

    //Hbase connector can only load configurations from a file path so we need to render the hadoop conf
    val confFile = Files.createTempFile("prefix", "suffix")
    val stream = Files.newOutputStream(confFile)
    minicluster.getConfiguration.writeXml(stream)
    stream.flush()
    stream.close()
    val hbaseConfigPath = ConfigValueFactory.fromAnyRef(confFile.toAbsolutePath.toString)

    //HbaseConnector will only load conf if hbase-site and core-site are given,
    //we give the same file to each.
    val config = ConfigFactory.load()
                              .withValue(ConfigurationKeys.HBASE_SITE, hbaseConfigPath)
                              .withValue(ConfigurationKeys.CORE_SITE, hbaseConfigPath)

    connector = new HBaseConnectorCreator().create(config)

    connector.createTable()
  }

  override def afterAll(): Unit = {
    minicluster.shutdown()
  }

}
