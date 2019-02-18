package it.agilelab.darwin.connector.hbase

import it.agilelab.darwin.common.{Connector, Logging, using}
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.JavaConverters._

case class HBaseConnector(connection: Connection,
                          namespace: String,
                          tableName: String) extends Connector with Logging {


  val TABLE_NAME: TableName = TableName.valueOf(Bytes.toBytes(namespace), Bytes.toBytes(tableName))

  val CF: Array[Byte] = Bytes.toBytes("0")
  val QUALIFIER_SCHEMA: Array[Byte] = Bytes.toBytes("schema")
  val QUALIFIER_NAME: Array[Byte] = Bytes.toBytes("name")
  val QUALIFIER_NAMESPACE: Array[Byte] = Bytes.toBytes("namespace")

  //TODO this must be a def (a new Parser is created each time) because if the same Parser is used, it fails if you
  //TODO parse a class A and after it a class B that has a field of type A => ERROR: Can't redefine type A.
  //TODO Sadly the Schema.parse() method that would solve this problem is now deprecated
  private def parser: Parser = new Parser()

  override def fullLoad(): Seq[(Long, Schema)] = {
    log.debug(s"loading all schemas from table $namespace:$tableName")
    val schemas = using(connection.getTable(TABLE_NAME).getScanner(CF, QUALIFIER_SCHEMA)) { scanner =>
      scanner.asScala.map { result =>
        val key = Bytes.toLong(result.getRow)
        val value = Bytes.toString(result.getValue(CF, QUALIFIER_SCHEMA))
        key -> parser.parse(value)
      }.toSeq
    }
    log.debug(s"${schemas.size} loaded from HBase")
    schemas
  }

  override def insert(schemas: Seq[(Long, Schema)]): Unit = {
    log.debug(s"inserting ${schemas.size} schemas in HBase table $namespace:$tableName")

    using(connection.getBufferedMutator(TABLE_NAME)) { mutator =>
      schemas.map { case (id, schema) =>
        val put = new Put(Bytes.toBytes(id))
        put.addColumn(CF, QUALIFIER_SCHEMA, Bytes.toBytes(schema.toString))
        put.addColumn(CF, QUALIFIER_NAME, Bytes.toBytes(schema.getName))
        put.addColumn(CF, QUALIFIER_NAMESPACE, Bytes.toBytes(schema.getNamespace))
        put
      }.foreach(mutator.mutate)
      mutator.flush()
    }
    log.debug(s"insertion of schemas into $namespace:$tableName successful")
  }

  override def createTable(): Unit = {
    using(connection.getAdmin) { admin =>
      if (!admin.listNamespaceDescriptors().exists(_.getName == namespace)) {
        log.info(s"Namespace $namespace does not exists, creating it")
        admin.createNamespace(NamespaceDescriptor.create(namespace).build())
      }
      if (!tableExists()) {
        log.info(s"Table $TABLE_NAME does not exists, creating it")
        admin.createTable(new HTableDescriptor(TABLE_NAME).addFamily(new HColumnDescriptor(CF)))
      }
    }
  }

  override def tableExists(): Boolean = {
    using(connection.getAdmin) { admin =>
      admin.tableExists(TABLE_NAME)
    }
  }

  override def tableCreationHint(): String = {
    s"""To create namespace and table from an HBase shell issue:
       |  create_namespace '$namespace'
       |  create '$namespace:$tableName', '0'""".stripMargin
  }

  override def findSchema(id: Long): Option[Schema] = {
    log.debug(s"loading a schema with id = $id from table $namespace:$tableName")
    val get: Get = new Get(Bytes.toBytes(id))
    get.addColumn(CF, QUALIFIER_SCHEMA)
    val result: Result = connection.getTable(TABLE_NAME).get(get)
    val value: Option[Array[Byte]] = Option(result.getValue(CF, QUALIFIER_SCHEMA))
    val schema: Option[Schema] = value.map(v => parser.parse(Bytes.toString(v)))
    log.debug(s"$schema loaded from HBase for id = $id")
    schema
  }
}
