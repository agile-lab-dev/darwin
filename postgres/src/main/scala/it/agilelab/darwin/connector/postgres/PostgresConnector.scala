package it.agilelab.darwin.connector.postgres

import java.sql.SQLException

import com.typesafe.config.Config
import it.agilelab.darwin.common.{Connector, using}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

import scala.util.{Failure, Success, Try}

class PostgresConnector(config: Config) extends Connector with PostgresConnection {

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
       |schema text NOT NULL,
       |name text,
       |namespace text
       |)""".stripMargin

  private val UPDATE_STMT = s"UPDATE $TABLE_NAME SET schema = ?, name = ?, namespace = ? WHERE id = ?"

  private val INSERT_STMT = s"INSERT INTO $TABLE_NAME (id, schema, name, namespace) VALUES (?,?,?,?)"

  override def fullLoad(): Seq[(Long, Schema)] = {
    using(getConnection) { connection =>
      val schemas = Seq.newBuilder[(Long, Schema)]
      val statement = connection.createStatement()
      using(statement.executeQuery(s"select * from $TABLE_NAME")) { resultSet =>
        while (resultSet.next()) {
          val id = resultSet.getLong("id")
          val schema = parser.parse(resultSet.getString("schema"))
          schemas += (id -> schema)
        }
        schemas.result()
      }
    }
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    val INS_ID: Int = 1
    val INS_SCHEMA: Int = 2
    val INS_NAME: Int = 3
    val INS_NAMESPACE: Int = 4
    val UPD_ID: Int = 4
    val UPD_SCHEMA: Int = 1
    val UPD_NAME: Int = 2
    val UPD_NAMESPACE: Int = 3
    using(getConnection) { connection =>
      connection.setAutoCommit(false)
      using(connection.prepareStatement(INSERT_STMT)) {
        insertSchemaPS =>
          using(connection.prepareStatement(UPDATE_STMT)) {
            updateSchemaPS =>
              schemas.foreach { case (id, schema) =>
                Try {
                  insertSchemaPS.setLong(INS_ID, id)
                  insertSchemaPS.setString(INS_SCHEMA, schema.toString)
                  insertSchemaPS.setString(INS_NAME, schema.getName)
                  insertSchemaPS.setString(INS_NAMESPACE, schema.getNamespace)
                  insertSchemaPS.executeUpdate()
                  connection.commit()
                }.recoverWith {
                  case _: SQLException =>
                    Try {
                      connection.rollback()
                      updateSchemaPS.setLong(UPD_ID, id)
                      updateSchemaPS.setString(UPD_SCHEMA, schema.toString)
                      updateSchemaPS.setString(UPD_NAME, schema.getName)
                      updateSchemaPS.setString(UPD_NAMESPACE, schema.getNamespace)
                      updateSchemaPS.executeUpdate()
                      connection.commit()
                    }
                } match {
                  case Failure(t) =>
                    connection.rollback()
                    throw t
                  case Success(_) =>
                }
              }
          }
      }
    }
  }

  override def findSchema(id: Long): Option[Schema] = {
    using(getConnection) { connection =>
      val statement = connection.prepareStatement(s"select * from $TABLE_NAME where id = ?")
      statement.setLong(1, id)
      using(statement.executeQuery()) { resultSet =>
        if (resultSet.next()) {
          Option(resultSet.getString("schema")).map(v => parser.parse(v))
        } else {
          None
        }
      }
    }
  }

  override def createTable(): Unit = {
    using(getConnection) {
      conn =>
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
