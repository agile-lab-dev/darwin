package it.agilelab.darwin.connector.hbase

import com.typesafe.config.Config
import it.agilelab.darwin.common.compat._
import it.agilelab.darwin.common.{Connector, DarwinEntry, Logging, using}
import org.apache.avro.Schema.Parser
import org.apache.commons.io.IOUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.security.User
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.security.UserGroupInformation

object HBaseConnector extends Logging {

  private var _instance: HBaseConnector = _

  def instance(hbaseConfig: Config): HBaseConnector = {
    synchronized {
      if (_instance == null) {
        log.debug("Initialization of HBase connector")
        _instance = HBaseConnector(hbaseConfig)
        log.debug("HBase connector initialized")
      }
    }
    _instance
  }
}

case class HBaseConnector(config: Config) extends Connector with Logging {

  val DEFAULT_NAMESPACE: String = "AVRO"
  val DEFAULT_TABLENAME: String = "SCHEMA_REPOSITORY"

  val TABLE_NAME_STRING: String = if (config.hasPath(ConfigurationKeys.TABLE)) {
    config.getString(ConfigurationKeys.TABLE)
  } else {
    DEFAULT_TABLENAME
  }

  val NAMESPACE_STRING: String = if (config.hasPath(ConfigurationKeys.NAMESPACE)) {
    config.getString(ConfigurationKeys.NAMESPACE)
  } else {
    DEFAULT_NAMESPACE
  }

  lazy val TABLE_NAME: TableName = TableName.valueOf(Bytes.toBytes(NAMESPACE_STRING), Bytes.toBytes(TABLE_NAME_STRING))

  val CF: Array[Byte] = Bytes.toBytes("0")
  val QUALIFIER_SCHEMA: Array[Byte] = Bytes.toBytes("schema")
  val QUALIFIER_NAME: Array[Byte] = Bytes.toBytes("name")
  val QUALIFIER_NAMESPACE: Array[Byte] = Bytes.toBytes("namespace")

  log.debug("Creating default HBaseConfiguration")
  val configuration: Configuration = HBaseConfiguration.create()
  log.debug("Created default HBaseConfiguration")

  if (config.hasPath(ConfigurationKeys.CORE_SITE) && config.hasPath(ConfigurationKeys.HBASE_SITE)) {
    log.debug(addResourceMessage(config.getString(ConfigurationKeys.CORE_SITE)))
    configuration.addResource(new Path(config.getString(ConfigurationKeys.CORE_SITE)))
    log.debug(addResourceMessage(config.getString(ConfigurationKeys.HBASE_SITE)))
    configuration.addResource(new Path(config.getString(ConfigurationKeys.HBASE_SITE)))
  }


  private def addResourceMessage(s: String) = {
    val ADDING_RESOURCE = "Adding resource: "
    ADDING_RESOURCE + s
  }

  val connection: Connection = if (config.getBoolean(ConfigurationKeys.IS_SECURE)) {
    log.debug(s"Calling UserGroupInformation.setConfiguration()")
    UserGroupInformation.setConfiguration(configuration)

    log.debug(s"Calling UserGroupInformation.loginUserFromKeytab(${config.getString(ConfigurationKeys.PRINCIPAL)}, " +
      s"${config.getString(ConfigurationKeys.KEYTAB_PATH)})")
    val ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(
      config.getString(ConfigurationKeys.PRINCIPAL), config.getString(ConfigurationKeys.KEYTAB_PATH)
    )
    UserGroupInformation.setLoginUser(ugi)
    val user = User.create(ugi)
    log.trace(s"initialization of HBase connection with configuration:\n " +
      s"${configuration.iterator().toScala.map { entry => entry.getKey -> entry.getValue }.mkString("\n")}")
    ConnectionFactory.createConnection(configuration, user)
  } else {
    log.trace(s"initialization of HBase connection with configuration:\n " +
      s"${configuration.iterator().toScala.map { entry => entry.getKey -> entry.getValue }.mkString("\n")}")
    ConnectionFactory.createConnection(configuration)
  }

  log.debug("HBase connection initialized")
  sys.addShutdownHook {
    //  log.info(s"closing HBase connection pool")
    IOUtils.closeQuietly(connection)
  }

  //TODO this must be a def (a new Parser is created each time) because if the same Parser is used, it fails if you
  //TODO parse a class A and after it a class B that has a field of type A => ERROR: Can't redefine type A.
  //TODO Sadly the Schema.parse() method that would solve this problem is now deprecated
  private def parser: Parser = new Parser()

  override def fullLoad(): Seq[DarwinEntry] = {
    log.debug(s"loading all schemas from table $NAMESPACE_STRING:$TABLE_NAME_STRING")
    val scanner: Iterable[Result] = connection.getTable(TABLE_NAME).getScanner(CF, QUALIFIER_SCHEMA).toScala
    val schemas = scanner.map { result =>
      val key = Bytes.toLong(result.getRow)
      val cell = result.getColumnLatestCell(CF, QUALIFIER_SCHEMA)
      val value = Bytes.toString(cell.getValueArray, cell.getValueOffset, cell.getValueLength)
      val version = cell.getTimestamp
      DarwinEntry(key, parser.parse(value), version)
    }.toSeq
    log.debug(s"${schemas.size} loaded from HBase")
    schemas
  }

  override def insert(schemas: Seq[DarwinEntry]): Unit = {
    val oldSchemas = fullLoad().groupBy(e => e.schemaFullName)
    val toInsert = schemas.flatMap { case e@DarwinEntry(fingerprint, _, _) =>
      oldSchemas.get(e.schemaFullName).fold(List(e)) { oldVersions =>
        if (oldVersions.exists(_.fingerprint == fingerprint)) {
          log.debug(s"schema with fingerprint ${fingerprint} already found in storage, skipping insert")
          Nil
        } else {
          List(e)
        }
      }
    }

    log.debug(s"inserting ${schemas.size} schemas in HBase table $NAMESPACE_STRING:$TABLE_NAME_STRING")

    val mutator = connection.getBufferedMutator(TABLE_NAME)
    toInsert.map { case DarwinEntry(id, schema, version) =>
      val put = new Put(Bytes.toBytes(id))
      put.addColumn(CF, QUALIFIER_SCHEMA, version, Bytes.toBytes(schema.toString))
      put.addColumn(CF, QUALIFIER_NAME, version, Bytes.toBytes(schema.getName))
      put.addColumn(CF, QUALIFIER_NAMESPACE, version, Bytes.toBytes(schema.getNamespace))
      put
    }.foreach(mutator.mutate)
    mutator.flush()
    log.debug(s"insertion of schemas into $NAMESPACE_STRING:$TABLE_NAME_STRING successful")
  }

  override def createTable(): Unit = {
    using(connection.getAdmin) { admin =>
      if (!admin.listNamespaceDescriptors().exists(_.getName == NAMESPACE_STRING)) {
        log.info(s"Namespace $NAMESPACE_STRING does not exists, creating it")
        admin.createNamespace(NamespaceDescriptor.create(NAMESPACE_STRING).build())
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
       |  create_namespace '$NAMESPACE_STRING'
       |  create '$NAMESPACE_STRING:$TABLE_NAME_STRING', '0'""".stripMargin
  }


  override def findSchema(id: Long): Option[DarwinEntry] = {
    log.debug(s"loading a schema with id = $id from table $NAMESPACE_STRING:$TABLE_NAME_STRING")
    val get: Get = new Get(Bytes.toBytes(id))
    get.addColumn(CF, QUALIFIER_SCHEMA)
    val result: Result = connection.getTable(TABLE_NAME).get(get)
    val value: Option[Cell] = Option(result.getColumnLatestCell(CF, QUALIFIER_SCHEMA))
    val entry: Option[DarwinEntry] = value.map(c =>
      DarwinEntry(id,
        parser.parse(Bytes.toString(c.getValueArray, c.getValueOffset, c.getValueLength)),
        c.getTimestamp)
    )
    log.debug(s"$entry loaded from HBase")
    entry
  }
}
