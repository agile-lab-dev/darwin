package it.agilelab.darwin.connector.mysql

import java.sql.ResultSet

import com.typesafe.config.Config
import it.agilelab.darwin.common.Connector
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

class MySQLConnector(config: Config) extends Connector(config) with MySQLConnection {

  private def parser: Parser = new Parser()

  private val DEFAULT_TABLENAME = "SCHEMA_REPOSITORY"

  val TABLE_NAME: String = if (config.hasPath(ConfigurationKeys.TABLE)) {
    config.getString(ConfigurationKeys.TABLE)
  } else {
    DEFAULT_TABLENAME
  }

  setConnectionConfig(config)

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
    connection.close
    schemas
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    val connection = getConnection
    try {
      connection.setAutoCommit(false)
      schemas.foreach { case (id, schema) =>
        val insertSchemaPS = connection.prepareStatement(s"INSERT INTO $TABLE_NAME (`id`,`schema`) VALUES (?,?)")
        insertSchemaPS.setLong(1, id)
        insertSchemaPS.setString(2, schema.toString)
        insertSchemaPS.executeUpdate()
        insertSchemaPS.close()
      }
      connection.commit
    } catch {
      case e: Exception => {
        connection.rollback
        // e.printStackTrace
        throw e // should re-throw?
      }
    } finally {
      connection.close
    }
  }
}
