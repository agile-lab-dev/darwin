package it.agilelab.darwin.connector.postgres

import java.sql.ResultSet

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, DarwinEntry, Logging, using}
import org.apache.avro.Schema.Parser

class PostgresConnector(config: Config) extends Connector with PostgresConnection with Logging {

  private def parser: Parser = new Parser()

  private val DEFAULT_TABLENAME = "SCHEMA_REPOSITORY"

  val TABLE_NAME: String = if (config.hasPath(ConfigurationKeys.TABLE)) {
    config.getString(ConfigurationKeys.TABLE)
  } else {
    DEFAULT_TABLENAME
  }

  setConnectionConfig(config)

  private val ID = "id"
  private val SCHEMA = "schema"
  private val VERSION = "version"
  private val NAME = "name"
  private val NAMESPACE = "namespace"
  private val CREATE_TABLE_STMT =
    s"""CREATE TABLE IF NOT EXISTS $TABLE_NAME (
       |$ID bigint NOT NULL PRIMARY KEY,
       |$SCHEMA text NOT NULL,
       |$NAME text,
       |$NAMESPACE text,
       |$VERSION bigint
       |)""".stripMargin

  override def fullLoad(): Seq[DarwinEntry] = {
    val connection = getConnection
    var schemas: Seq[DarwinEntry] = Seq.empty
    val statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(s"select * from $TABLE_NAME")

    while (resultSet.next()) {
      val id = resultSet.getLong(ID)
      val schema = parser.parse(resultSet.getString(SCHEMA))
      val version = Option(resultSet.getLong(VERSION)).getOrElse(0L)
      schemas = schemas :+ DarwinEntry(id, schema, version)
    }
    connection.close()
    schemas
  }

  override def insert(schemas: Seq[DarwinEntry]): Unit = {
    val oldSchemas = fullLoad().groupBy(e => e.schemaFullName)
    val toInsert = schemas.flatMap { case e@DarwinEntry(fingerprint, _, _) =>
      oldSchemas.get(e.schemaFullName).fold(List(e)) { oldVersions =>
        if (oldVersions.exists(_.fingerprint == fingerprint)) {
          log.debug(s"$SCHEMA with fingerprint ${fingerprint} already found in storage, skipping insert")
          Nil
        } else {
          List(e)
        }
      }
    }
    val connection = getConnection
    val ID_IDX: Int = 1
    val SCHEMA_IDX: Int = 2
    val NAME_IDX: Int = 3
    val NAMESPACE_IDX: Int = 4
    val VERSION_IDX: Int = 5
    try {
      connection.setAutoCommit(false)
      toInsert.foreach { case DarwinEntry(id, schema, version) =>
        val insertSchemaPS = connection
          .prepareStatement(s"INSERT INTO $TABLE_NAME ($ID, $SCHEMA, $NAME, $NAMESPACE, $VERSION)" +
            s" VALUES (?,?,?,?,?)")
        insertSchemaPS.setLong(ID_IDX, id)
        insertSchemaPS.setString(SCHEMA_IDX, schema.toString)
        insertSchemaPS.setString(NAME_IDX, schema.getName)
        insertSchemaPS.setString(NAMESPACE_IDX, schema.getNamespace)
        insertSchemaPS.setLong(VERSION_IDX, version)
        insertSchemaPS.executeUpdate()
        insertSchemaPS.close()
      }
      connection.commit()
    } catch {
      case e: Exception =>
        connection.rollback()
        throw e
    } finally {
      connection.close()
    }
  }

  override def findSchema(id: Long): Option[DarwinEntry] = {
    val connection = getConnection
    val statement = connection.prepareStatement(s"select * from $TABLE_NAME where $ID = ?")
    statement.setLong(1, id)
    val resultSet: ResultSet = statement.executeQuery()

    val entry = if (resultSet.next()) {
      val schema = resultSet.getString(SCHEMA)
      val id = resultSet.getLong(ID)
      val version = resultSet.getLong(VERSION)
      Some(DarwinEntry(id, parser.parse(schema), version))
    } else {
      None
    }
    connection.close()
    entry
  }

  override def createTable(): Unit = {
    using(getConnection) { conn =>
      conn.createStatement().executeUpdate(CREATE_TABLE_STMT)
    }
  }

  override def tableExists(): Boolean = false // FIX IT PLS

  override def tableCreationHint(): String = {
    s"""To create table perform the following sql query:
       |$CREATE_TABLE_STMT
     """.stripMargin
  }
}
