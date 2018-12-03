package it.agilelab.darwin.connector.postgres

import java.sql.ResultSet

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, using}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

class PostgresConnector(config: Config) extends Connector(config) with PostgresConnection {

  private def parser: Parser = new Parser()

  private val DEFAULT_TABLENAME = "SCHEMA_REPOSITORY"

  val TABLE_NAME: String = if (config.hasPath(ConfigurationKeys.TABLE)) {
    config.getString(ConfigurationKeys.TABLE)
  } else {
    DEFAULT_TABLENAME
  }

  setConnectionConfig(config)

  private val CREATE_TABLE_STMT =
    s"""CREATE TABLE IF NOT EXISTS $TABLE_NAME (
       |id bigint NOT NULL PRIMARY KEY,
       |schema text NOT NULL
       |)""".stripMargin

  override def fullLoad(): Seq[(Long, Schema)] = {
    val connection = getConnection
    var schemas: Seq[(Long, Schema)] = Seq.empty[(Long, Schema)]
    val statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(s"select * from $TABLE_NAME")

    while (resultSet.next()) {
      val id = resultSet.getLong("id")
      val schema = parser.parse(resultSet.getString("schema"))
      schemas = schemas :+ (id -> schema)
    }
    connection.close()
    schemas
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    val connection = getConnection
    try {
      connection.setAutoCommit(false)
      schemas.foreach { case (id, schema) =>
        val insertSchemaPS = connection.prepareStatement(s"INSERT INTO $TABLE_NAME (id,schema) VALUES (?,?)")
        insertSchemaPS.setLong(1, id)
        insertSchemaPS.setString(2, schema.toString)
        insertSchemaPS.executeUpdate()
        insertSchemaPS.close()
      }
      connection.commit()
    } catch {
      case e: Exception =>
        connection.rollback()
        // e.printStackTrace
        throw e // should re-throw?
    } finally {
      connection.close()
    }
  }

  override def findSchema(id: Long): Option[Schema] = {
    val connection = getConnection
    val statement = connection.prepareStatement(s"select * from $TABLE_NAME where id = ?")
    statement.setLong(1, id)
    val resultSet: ResultSet = statement.executeQuery()

    val schema = if(resultSet.next()) {
      Option(resultSet.getString("schema")).map(v => parser.parse(v))
    } else {
      None
    }
    connection.close()
    schema
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
