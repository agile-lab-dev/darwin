package it.agilelab.darwin.connector.postgres

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.Connector
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class PostgresConnectorSuite extends FlatSpec with Matchers with BeforeAndAfterAll {
  val connector: Connector = new PostgresConnectorCreator().create(ConfigFactory.load("postgres.properties"))


  "PostgresConnector" should "load all existing schemas" in {
    connector.fullLoad()
  }

  ignore should "insert and retrieve" in {
    val outerSchema = new Schema.Parser().parse(getClass.getClassLoader.getResourceAsStream("postgresmock.avsc"))
    val innerSchema = outerSchema.getField("four").schema()

    val schemas = Seq(innerSchema, outerSchema)
      .map(s => SchemaNormalization.parsingFingerprint64(s) -> s)
    connector.insert(schemas)
    val loaded: Seq[(Long, Schema)] = connector.fullLoad()
    assert(loaded.size == schemas.size)
    assert(loaded.forall(schemas.contains))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    connector.createTable()

  }
}
