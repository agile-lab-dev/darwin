package it.agilelab.darwin.connector.postgres

import com.typesafe.config.ConfigFactory
import it.agilelab.darwin.common.Connector
import org.apache.avro.{Schema, SchemaNormalization}
import org.scalatest.{FlatSpec, Matchers}

class PostgresConnectorSuite extends FlatSpec with Matchers{
  val connector: Connector = new PostgresConnectorCreator().create(ConfigFactory.load("postgres.properties"))

  "PostgresConnector" should "load all existing schemas" in {
    connector.fullLoad()
  }

  it should "insert and retrieve" in {
    val schemas = Seq(new SchemaGenerator[PostgresMock].schema, new SchemaGenerator[Postgres2Mock].schema)
      .map(s => SchemaNormalization.parsingFingerprint64(s) -> s)
    connector.insert(schemas)
    val loaded: Seq[(Long, Schema)] = connector.fullLoad()
    assert(loaded.size == schemas.size)
    assert(loaded.forall(schemas.contains))
  }

}
