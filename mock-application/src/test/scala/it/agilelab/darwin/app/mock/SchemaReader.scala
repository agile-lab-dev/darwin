package it.agilelab.darwin.app.mock

import java.io.{File, InputStream}

import org.apache.avro.Schema

object SchemaReader {

  def readFromResources(p: String): Schema = {
    read(getClass.getClassLoader.getResourceAsStream(p))
  }

  def read(f: File): Schema = {
    val parser = new Schema.Parser()
    parser.parse(f)
  }

  def read(s: String): Schema = {
    val parser = new Schema.Parser()
    parser.parse(s)
  }

  def read(is: InputStream): Schema = {
    val parser = new Schema.Parser()
    parser.parse(is)
  }
}
