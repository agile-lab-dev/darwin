package it.agilelab.darwin.connector.postgres

import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }
import it.agilelab.darwin.common.Connector
import org.apache.avro.{ Schema, SchemaNormalization }
import org.scalatest.BeforeAndAfterAll
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres
import ru.yandex.qatools.embed.postgresql.distribution.Version
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PostgresConnectorSuite extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  val embeddedPostgres: EmbeddedPostgres = new EmbeddedPostgres(Version.V9_6_11)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val port                 = 5432
    val host                 = "localhost"
    val dbname               = "postgres"
    val username             = "postgres"
    val password             = "mysecretpassword"
    embeddedPostgres.start(host, port, dbname, username, password)
    val config: Config       = ConfigFactory.load("postgres.properties")
    val connector: Connector = new PostgresConnectorCreator().create(config)
    connector.createTable()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    embeddedPostgres.stop()
  }

  it should "multiple insert and retrieve [No conf - OneTransaction]" in {
    val config: Config       = ConfigFactory.load("postgres.properties")
    val connector: Connector = new PostgresConnectorCreator().create(config)
    test(connector)
  }

  it should "multiple insert and retrieve [OneTransaction]" in {
    val config: Config       = ConfigFactory
      .load("postgres.properties")
      .withValue(ConfigurationKeys.MODE, ConfigValueFactory.fromAnyRef(OneTransaction.value))
    val connector: Connector = new PostgresConnectorCreator().create(config)
    test(connector)
  }

  it should "multiple insert and retrieve [ExceptionDriven]" in {
    val config: Config       = ConfigFactory
      .load("postgres.properties")
      .withValue(ConfigurationKeys.MODE, ConfigValueFactory.fromAnyRef(ExceptionDriven.value))
    val connector: Connector = new PostgresConnectorCreator().create(config)
    test(connector)
  }

  private def test(connector: Connector) = {
    val outerSchema                 = new Schema.Parser().parse(getClass.getClassLoader.getResourceAsStream("postgresmock.avsc"))
    val innerSchema                 = outerSchema.getField("four").schema()
    val schemas                     = Seq(innerSchema, outerSchema)
      .map(s => SchemaNormalization.parsingFingerprint64(s) -> s)
    connector.insert(schemas)
    connector.insert(schemas)
    connector.insert(schemas)
    connector.insert(schemas)
    val loaded: Seq[(Long, Schema)] = connector.fullLoad()
    assert(loaded.size == schemas.size)
    assert(loaded.forall(schemas.contains))
  }
}
