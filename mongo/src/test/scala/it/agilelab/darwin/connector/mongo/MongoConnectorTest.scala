package it.agilelab.darwin.connector.mongo

import com.typesafe.config.{ Config, ConfigFactory }
import de.flapdoodle.embed.mongo.{ MongodExecutable, MongodProcess, MongodStarter }
import de.flapdoodle.embed.mongo.config.{ IMongodConfig, MongodConfigBuilder, Net }
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import org.scalatest.BeforeAndAfterAll
import org.mongodb.scala.MongoClient
import it.agilelab.darwin.common.Connector
import it.agilelab.darwin.connector.mongo.ConfigurationMongoModels.MongoConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MongoConnectorTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val port                               = 12345
  val config: Config                     = ConfigFactory.load("mongo.conf")
  val starter: MongodStarter             = MongodStarter.getDefaultInstance
  val mongodConfig: IMongodConfig        =
    new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", port, Network.localhostIsIPv6))
      .build
  val mongoConfig: MongoConfig           = MongoConfig(
    config.getString(ConfigurationKeys.DATABASE),
    config.getString(ConfigurationKeys.COLLECTION),
    if (config.hasPath(ConfigurationKeys.TIMEOUT)) {
      Duration.create(config.getInt(ConfigurationKeys.TIMEOUT), "millis")
    } else {
      Duration.create(ConfigurationMongoModels.DEFAULT_DURATION, "millis")
    }
  )
  val mongodExecutable: MongodExecutable = starter.prepare(mongodConfig)
  var mongod: MongodProcess              = _
  var mongoClient: MongoClient           = _
  var connector: Connector               = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    mongod = mongodExecutable.start
    mongoClient = MongoClient(
      s"mongodb://${config.getStringList(ConfigurationKeys.HOST).get(0)}/" +
        s"${config.getString(ConfigurationKeys.DATABASE)}"
    )
    connector = new MongoConnector(mongoClient, mongoConfig)
    connector.createTable()
  }

  override protected def afterAll(): Unit = {
    mongod.stop()

    super.afterAll()
  }

  "Table collection_test" should "be created by connector" in {
    connector.createTable()
    assert(connector.tableExists())
  }

  "schemas" should "be inserted into collection" in {
    val schema: Schema    = new Parser().parse(getClass.getClassLoader.getResourceAsStream("mongomock.avsc"))
    val schemas           = Seq((0L, schema), (1L, schema))
    connector.insert(schemas)
    val numberOfDocuments =
      Await.result(
        mongoClient
          .getDatabase(config.getString(ConfigurationKeys.DATABASE))
          .getCollection(config.getString(ConfigurationKeys.COLLECTION))
          .countDocuments()
          .toFuture(),
        mongoConfig.timeout
      )
    assert(numberOfDocuments == 2)
  }

  "schema" should "not be inserted into collection because because there is already a scheme with the same id" in {
    val schema: Schema    = new Parser().parse(getClass.getClassLoader.getResourceAsStream("mongomock.avsc"))
    val schemas           = Seq((0L, schema))
    connector.insert(schemas)
    val numberOfDocuments =
      Await.result(
        mongoClient
          .getDatabase(config.getString(ConfigurationKeys.DATABASE))
          .getCollection(config.getString(ConfigurationKeys.COLLECTION))
          .countDocuments()
          .toFuture(),
        mongoConfig.timeout
      )
    assert(numberOfDocuments == 2)
  }

  "full load" should "return a list of lenght equals to 2" in {
    val schemas: Seq[(Long, Schema)] = connector.fullLoad()
    assert(schemas.length == 2)
  }

  "find schema" should "return a schema" in {
    val schema: Option[Schema] = connector.findSchema(0L)
    assert(schema.isDefined)
  }

  "find schema" should "return a None" in {
    val schema: Option[Schema] = connector.findSchema(3L)
    assert(schema.isEmpty)
  }

}
