package it.agilelab.darwin.connector.mock

import com.typesafe.config.Config
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{ Connector, Logging, SchemaReader }
import org.apache.avro.{ Schema, SchemaNormalization }

import scala.collection.mutable

class MockConnectorException(msg: String, t: Option[Throwable]) extends RuntimeException(msg) {
  def this(msg: String) = this(msg, None)

  def this(t: Throwable) = this(t.getMessage, Some(t))

  override def getCause: Throwable = t match {
    case Some(value) => value
    case None        => super.getCause
  }
}

class MockConnector(config: Config) extends Connector with Logging {

  private[this] var loaded: Boolean = false

  val mode: ConfigurationKeys.Mode = if (config.hasPath(ConfigurationKeys.MODE)) {
    ConfigurationKeys.Mode.parse(config.getString(ConfigurationKeys.MODE))
  } else {
    ConfigurationKeys.Strict
  }

  private def files = if (config.hasPath(ConfigurationKeys.FILES)) {
    config.getStringList(ConfigurationKeys.FILES).toScala().map { s =>
      try {
        SchemaReader.safeRead(new java.io.File(s))
      } catch {
        case t: Throwable => Left(SchemaReader.UnknownError(t))
      }
    }
  } else {
    Nil
  }

  private def resources = if (config.hasPath(ConfigurationKeys.RESOURCES)) {
    config.getStringList(ConfigurationKeys.RESOURCES).toScala().map { s =>
      try {
        SchemaReader.safeReadFromResources(s)
      } catch {
        case t: Throwable => Left(SchemaReader.UnknownError(t))
      }
    }
  } else {
    Nil
  }

  private def handleError(error: SchemaReader.SchemaReaderError): Unit = {
    mode match {
      case ConfigurationKeys.Strict     =>
        error match {
          case SchemaReader.SchemaParserError(exception) =>
            throw new MockConnectorException(exception)
          case SchemaReader.IOError(exception)           => throw new MockConnectorException(exception)
          case SchemaReader.ResourceNotFoundError(msg)   => throw new MockConnectorException(msg)
          case SchemaReader.UnknownError(t)              => throw new MockConnectorException(t)
        }
      case ConfigurationKeys.Permissive =>
        error match {
          case SchemaReader.SchemaParserError(exception) => log.warn(exception.getMessage, exception)
          case SchemaReader.IOError(exception)           => log.warn(exception.getMessage, exception)
          case SchemaReader.ResourceNotFoundError(msg)   => log.warn(msg)
          case SchemaReader.UnknownError(t)              => log.warn(t.getMessage, t)
        }
    }
  }

  private val table: mutable.Map[Long, Schema] = mutable.Map.empty[Long, Schema]

  override def fullLoad(): Seq[(Long, Schema)] = {
    (resources ++ files).foreach {
      case Left(error)   => handleError(error)
      case Right(schema) => table(SchemaNormalization.parsingFingerprint64(schema)) = schema
    }
    table.toSeq
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    schemas.foreach { case (id, schema) =>
      table(id) = schema
    }
  }

  override def findSchema(id: Long): Option[Schema] = {
    if (!loaded) {
      this.synchronized {
        if (!loaded) {
          fullLoad()
          loaded = true
        }
      }
    }
    table.get(id)
  }

  override def createTable(): Unit = ()

  override def tableExists(): Boolean = true

  override def tableCreationHint(): String = "No table needs to be created since mock connecto"
}
