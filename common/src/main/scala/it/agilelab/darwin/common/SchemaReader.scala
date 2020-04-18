package it.agilelab.darwin.common

import java.io.{File, IOException, InputStream}

import org.apache.avro.{Schema, SchemaParseException}

object SchemaReader {

  def readFromResources(p: String): Schema = {
    using(getClass.getClassLoader.getResourceAsStream(p)) { stream =>
      read(stream)
    }
  }

  def read(f: File): Schema = {
    val parser = new Schema.Parser()
    parser.parse(f)
  }

  def read(s: String): Schema = {
    val parser = new Schema.Parser()
    parser.parse(s)
  }

  /**
    * Does not close the InputStream
    */
  def read(is: InputStream): Schema = {
    val parser = new Schema.Parser()
    parser.parse(is)
  }

  def safeReadFromResources(p: String): Either[SchemaReaderError, Schema] = {
    Option(getClass.getClassLoader.getResourceAsStream(p)).fold[Either[SchemaReaderError, Schema]](
      Left(ResourceNotFoundError(s"Cannot find resource: $p"))
    ) { stream =>
      try {
        safeRead(stream)
      } catch {
        case e: SchemaParseException => Left(SchemaParserError(e))
        case e: IOException => Left(IOError(e))
        case e: Throwable => Left(UnknownError(e))
      } finally {
        stream.close()
      }
    }
  }

  def safeRead(f: File): Either[SchemaReaderError, Schema] = {
    try {
      Right(new Schema.Parser().parse(f))
    } catch {
      case e: SchemaParseException => Left(SchemaParserError(e))
      case e: IOException => Left(IOError(e))
      case e: Throwable => Left(UnknownError(e))
    }
  }

  def safeRead(s: String): Either[SchemaReaderError, Schema] = {
    try {
      Right(new Schema.Parser().parse(s))
    } catch {
      case e: SchemaParseException => Left(SchemaParserError(e))
      case e: IOException => Left(IOError(e))
      case e: Throwable => Left(UnknownError(e))
    }
  }

  /**
    * Does not close the InputStream
    */
  def safeRead(is: InputStream): Either[SchemaReaderError, Schema] = {
    try {
      Right(new Schema.Parser().parse(is))
    } catch {
      case e: SchemaParseException => Left(SchemaParserError(e))
      case e: IOException => Left(IOError(e))
      case e: Throwable => Left(UnknownError(e))
    }
  }

  sealed trait SchemaReaderError

  case class SchemaParserError(exception: SchemaParseException) extends SchemaReaderError

  case class IOError(exception: IOException) extends SchemaReaderError

  case class ResourceNotFoundError(msg: String) extends SchemaReaderError

  case class UnknownError(t: Throwable) extends SchemaReaderError

}
