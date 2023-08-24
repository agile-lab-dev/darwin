# Darwin

[![CI](https://github.com/agile-lab-dev/darwin/actions/workflows/ci.yml/badge.svg)](https://github.com/agile-lab-dev/darwin/actions/workflows/ci.yml)
[![Gitter](https://badges.gitter.im/agile-lab-darwin/community.svg)](https://gitter.im/agile-lab-darwin/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

<img align="right" height="260" src="docs/img/logo/darwin-icon.svg">

Table of contents
-------------

- General
  - [Overview](#overview)
  - [Artifacts](#artifacts)
  - [Background](#background)
  - [Architecture](#architecture)
  - [JVM compatibility](#jvm-compatibility)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
  - [General](#general)
  - [HBase](#hbase)
  - [PostgreSql](#postgresql)
  - [REST](#rest)
  - [Confluent](#confluent)
---

Overview
-------------
Darwin is a repository of Avro schemas that maintains all the schema versions used during your application lifetime.
Its main goal is to provide an easy and transparent access to the Avro data in your storage independently from 
schemas evolutions.
Darwin is portable and it doesn't require any application server.
To store its data, you can choose from multiple storage managers (HBase, Postgres) easily pluggable importing the 
desired connector.

Artifacts
--------------
Darwin artifacts are published for scala 2.10, 2.11, 2.12 and 2.13 (from version 1.0.12). 
From version 1.0.2 Darwin is available from maven central so there is no need to configure additional repositories 
in your project.

In order to access to Darwin core functionalities add the core dependency to you project:

### core

#### sbt
```scala
libraryDependencies += "it.agilelab" %% "darwin-core" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-core_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```

### HBase connector

Then add the connector of your choice, either HBase:

#### sbt
```scala
libraryDependencies += "it.agilelab" %% "darwin-hbase-connector" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-hbase-connector_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```

### Postgresql connector

Or PostgreSql:

### sbt

```scala
libraryDependencies += "it.agilelab" %% "darwin-postgres-connector" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-postgres-connector_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```


### Rest Connector

Or Rest

### sbt

```scala
libraryDependencies += "it.agilelab" %% "darwin-rest-connector" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-rest-connector_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```

#### Rest server

To use the rest connector implement the required endpoints or use the reference implementation provided by rest-server module

### Mock connector

Or Mock (only for test scenarios):

### sbt

```scala
libraryDependencies += "it.agilelab" %% "darwin-mock-connector" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-mock-connector_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```


### Confluent schema registry Connector

Darwin can be used as a *facade* over confluent schema registry.

### sbt

```scala
libraryDependencies += "it.agilelab" %% "darwin-confluent-connector" % "1.2.1-SNAPSHOT"
```
#### maven
```xml
<dependency>
  <groupId>it.agilelab</groupId>
  <artifactId>darwin-confluent-connector_2.11</artifactId>
  <version>1.2.1-SNAPSHOT</version>
</dependency>
```


Background
-------------
In systems where objects encoded using Avro are stored, a problem arises when there is an evolution of the structure 
of those objects. In these cases, Avro is not capable of reading the old data using the schema extracted from the 
actual version of the object: in this scenario each avro-encoded object must be stored along with its schema. 
To address this problem Avro defined the [Single-Object Encoding specification](https://avro.apache.org/docs/1.8.2/spec.html#single_object_encoding_spec):
>### Single-object encoding
>In some situations a single Avro serialized object is to be stored for a longer period of time.
>In the period after a schema change this persistance system will contain records that have been written with 
different schemas. So the need arises to know which schema was used to write a record to support schema evolution correctly.
In most cases the schema itself is too large to include in the message, so this binary wrapper format supports the use case more effectively.

Darwin is compliant to this specification and provides utility methods that can generate a Single-Object encoded from
 an Avro byte array and extract an Avro byte array (along with its schema) from a Single-Object encoded one.

Architecture
-------------
### Darwin architecture schema
Darwin maintains a repository of all the known schemas in the configured storage, and can access these data in three 
configurable ways:
1. ##### Eager Cached
    Darwin loads all schemas once from the selected storage and fills with them an internal cache that is used for 
    all the subsequent queries. The only other access to the storage is due to the invocation of the `registerAll` 
    method which updates both the cache and the storage with the new schemas. Once the cache is loaded, all the 
    `getId` and `getSchema` method invocations will perform lookups only in the cache.

    ![Darwin schema](docs/img/darwin_eager_cached_schema.jpg)

2. ##### Lazy Cached
    Darwin behaves like the Eager Cached scenario, but each cache miss is then attempted also into the storage. If 
    the data is found on the storage, the cache is then updated with the fetched data.
    
    ![Darwin schema](docs/img/darwin_lazy_cached_schema.jpg)
    
3. ##### Lazy
    Darwin performs all lookups directly on the storage: there is no applicative cache.
    
    ![Darwin schema](docs/img/darwin_lazy_schema.jpg)

### Darwin interaction
Darwin can be used to easily read and write data encoded in Avro Single-Object using the 
`generateAvroSingleObjectEncoded` and `retrieveSchemaAndAvroPayload` methods of a `AvroSchemaManager` instance (they 
rely on the `getId` and `getSchema` methods discussed before). These methods allow your application to convert and 
encoded avro byte array into a single-object encoded one, and to extract the schema and payload from a single-object 
encoded record that was written.
If there is the need to use single-object encoding utilities without creating an `AvroSchemaManager` instance, the 
utilities object `AvroSingleObjectEncodingUtils` exposes some generic purpose functionality, such as:
- check if a byte array is single-object encoded
- create a single-object encoded byte array from payload and schema ID
- extract the schema ID from a single-object encoded byte array
- remove the header (schema ID included) of a single-object encoded byte array

![Darwin interaction](docs/img/darwin_interaction.jpg)

JVM compatibility
-------------
Darwin is cross-published among different scala versions (2.10, 2.11, 2.12, 2.13).
Depending on the Scala version, it targets different JVM versions.

Please refer to the following compatibility matrix:

| Scala version | JVM version |
|---------------|-------------|
| 2.10 | 1.7 |
| 2.11 | 1.7 |
| 2.12 | 1.8 |
| 2.13 | 1.8 |

Installation
-------------
To use Darwin in your application, simply add it as dependency along with one of the available connectors.
Darwin can automatically load the defined connector, and it can be used directly to register and to retrieve 
Avro schemas.

Usage
-------------
Darwin main functionality are exposed by the `AvroSchemaManager`, which can be used to store and retrieve the known 
avro schemas.
To get an instance of `AvroSchemaManager` there are two main ways:
1. You can create an instance of `AvroSchemaManager` directly, passing a `Connector` as constructor argument; the 
available implementations of `AvroSchemaManager` are the ones introduced in te chapter [Architecture](#architecture):
 `CachedEagerAvroSchemaManager`, `CachedLazyAvroSchemaManager` and `LazyAvroSchemaManager`.
2. You can obtain an instance of `AvroSchemaManager` using the `AvroSchemaManagerFactory`: for each configuration 
passed as input of the `initialize` method, a new instance is created. The instance can be retrieved later using the 
`getInstance` method.

To get more insight on how the Typesafe configuration must be defined to create an `AvroSchemaManager` instance (or 
directly a `Connector` instance), please check how the configuration file should be created in the Configuration 
section of the storage you chose.

Once you created an instance of `AvroSchemaManager`, first of all an application should register all its known Avro
 schemas invoking the `registerAll` method:
```
  val manager: AvroSchemaManager = AvroSchemaManagerFactory.initialize(config)
  val schemas: Seq[Schema] = //obtain all the schemas
  val registered: Seq[(Long, Schema)] = manager.registerAll(schemas)
```
To generate the Avro schema for your classes there are various ways, if you are using standard Java pojos:
```
  val schema: Schema = ReflectData.get().getSchema(classOf[MyClass])
```
If your application uses the _avro4s library_ you can instead obtain the schemas through the `AvroSchema` typeclass 
implicitly generated by _avro4s_, e.g.:
```
  val schema: Schema = new AvroSchema[MyClass]
```
Once you have registered all the schemas used by your application, you can use them directly invoking the 
`AvroSchemaManager` object: it exposes functionality to retrieve the schema from an ID and vice-versa.
```
  val id: Long = manager.getId(schema)
  val schema: Schema = manager.getSchema(id)
```

As said previously, in addition to the basic methods, the `AvroSchemaManager` object exposes also some utility methods 
that can be used to encode/decode a byte array in single-object encoding:
```
  def generateAvroSingleObjectEncoded(avroPayload: Array[Byte], schema: Schema): Array[Byte]

  def retrieveSchemaAndAvroPayload(avroSingleObjectEncoded: Array[Byte]): (Schema, Array[Byte])
```

If new schemas are added to the storage and the application must reload all the data from it (in order to manage also
 objects encoded with the new schemas), the `reload` method can be used:
 ```
 manager.reload()
 ```
 Please note that this method can be used to reload all the schemas in cached scenarios (this method does nothing if 
 you are using a `LazyAvroSchemaManager` instance, because all the find are performed directly on the storage).

Configuration
-------------

## General

The general configuration keys are:
- **endianness**: tells the factory the endianness which will be used to store and parse schema fingerprints.
Allowed values are: "LITTLE_ENDIAN" and "BIG_ENDIAN".
- **type**: tells the factory which instance of `AvroSchemaManager` must be created. Allowed values are: 
"cached_eager", "cached_lazy" and "lazy".
- **connector** (optional): used to choose the connector if there are multiple instances of connectors found at 
runtime. If multiple instances are found and this key is not configured, the first connector is taken. All available 
connectors names are suitable for this value (e.g. "hbase", "postgresql", etc)
- **createTable** (optional): if true, tells the chosen Connector to create the repository table if not already 
present in the storage.

## HBase

The configuration keys managed by the `HBaseConnector` are:
- **namespace** (optional): namespace of the table used by Darwin to store the schema repository (if it isn't set, 
the default value "AVRO" is used)
- **table** (optional): name of the table used by Darwin to store the schema repository (if it isn't set, the default
 value "SCHEMA_REPOSITORY" is used)
- **coreSite** (optional): path of the core-site.xml file (not mandatory if the file is already included in the 
classpath)
- **hbaseSite** (optional): path of the hbase-site.xml file (not mandatory if the file is already included in the 
classpath)
- **isSecure**: true if the HBase database is kerberos-secured
- **keytabPath** (optional): path to the keytab containing the key for the principal
- **principal** (optional): name of the principal, usually in the form of `primary/node@REALM`

Example of configuration for the `HBaseConnector`:
```
"isSecure": false,
"namespace": "DARWIN",
"table": "REPOSITORY",
"coreSite": "/etc/hadoop/conf/core-site.xml",
"hbaseSite": "/etc/hadoop/conf/hbase-site.xml",
```
### HBase Connector dependencies
Darwin HBase Connector does not provide HBase dependencies in a transitive manner since that would lead to hard to 
manage classpath and class versions conflicts (see Maven hell). Therefore it is mandatory to include also HBase 
dependencies into your project. 

## Postgresql

The configuration keys managed by the `PostgresConnector` are:
- **table** (optional): name of the table used by Darwin to store the schema repository (if it isn't set, the default
 value "SCHEMA_REPOSITORY" is used)
- **host**: the host of the PostgreSql database
- **db**: the name of the database where the table will be looked for
- **username**: the user to connect to PostgreSql
- **password**: the password of the user to connect to PostgreSql
- **mode**: controls the way upserts are implemented, either with a single transaction (transaction), or trying to 
insert and then falling back to update when key violation is raised (exception). default: `transaction` 

Example of configuration for the `PostgresConnector`:
```
"host": "localhost:5432"
"db": "srdb"
"username": "postgres"
"password": "srpsql"
"table": "schema_registry"
"mode": "transaction"
```

## MongoDB

MongoDB Connector works with scala 2.11, 2.12 and 2.13.

MongoDB dependencies added for creating this connector should be provided by the user.

There are two possibilities to create a `MongoConnector`:
- Use the MongoConnectorCreator: A connection to MongoDB will be created by reading the information from a configuration file
- Use the MongoConnector constructor: Create a MongoConnector. This constructor allows you to build a connector and pass a user-customized connection to it.

The configuration keys managed by the `MongoConnectorCreator` are:
- **collection**: name of the collection used by Darwin to store the schema repository
- **host**: list of the hosts where you want to connect
- **database**: the name of the database where the table will be looked for
- **username**: the user to connect to MongoDB
- **password**: the password of the user to connect to MongoDB
- **timeout**: maximum waiting time in milliseconds to obtain the results

The configuration keys managed by the `MongoConnector` are:
- **collection**: name of the collection used by Darwin to store the schema repository
- **database**: the name of the database where the table will be looked for
- **timeout**: maximum waiting time in milliseconds to obtain the results.

Entering other configuration keys will not lead to errors, they will not simply be considered.

```
username = "mongo"
password = "mongo"
host = ["localhost:12345"]
database = "test"
collection = "collection_test"
timeout = 5000
```

## REST

The configuration keys managed by the `RestConnector` are:
- **protocol**: http or https
- **host**: the hostname where rest-server (or an http proxy) is deployed
- **port**: the port where rest-server (or an http proxy) is listening
- **basePath**: the path that should be prefixed to all requests (useful if rest-server is running behind a reverse proxy)

Example of configuration for the `RestConnector`:
```
"protocol": "http"
"host": "localhost"
"port": 8080
"basePath": "/"
```

### REST Server

A rest server is provided by module rest-server (only for scala 2.11, 2.12 and 2.13), just run main class

`it.agilelab.darwin.server.rest.Main`

### REST Server configuration

The same configuration options of darwin as a library should be configured under the darwin key.

The rest server also accepts a rest specific configuration under darwin-rest key.

Example configuration for the `RestServer`:

```
darwin {
  type = "lazy"
  connector = "mock"
}

darwin-rest {
  interface = "localhost"
  port = 8080
}
```

## Confluent

Darwin can be used as a `facade` over the `Confluent schema registry`.

Connecting to the confluent schema registry will help all applications currently using darwin to function correctly
when running over confluent platform.

The connector can be used even if the only confluent component used is the schema registry.

When using the confluent connector a the avro single object encoding will be performed using the *Confluent* flavour.

### Confluent Single object encoding

The schema registry will assign globally unique ids to schemas, each avro message is encoded as following

```
0x00                       |   1 byte magic number representing confluent encoded avro
0xXX 0xXX 0xXX 0xXX        |   4 byte schema identifier interpreted as an integer
...                        |   avro encoded payload without schema (raw avro bytes not prepended with the json schema)
```

### Subject

Confluent schema registry supports attaching schemas to a `subject`, the subject is the granularity at which schema
compatibility is enforced, schemas can be registered with 3 subject strategies

* topic: The subject is the name of the topic (topic contains a single avro data type)
* record: The subject is the fully qualified name of the topic (multiple topics can contain the same avro data type)
* topic-record: The subject is derived from topic and record fqdn (a topic can have multiple data types, compatibility on 
same avro data type will be enforced for each topic instead of globally)

In order to support this scheme avro schemas registered via darwin should have a custom extension (`x-darwin-subject`)
like in this example

```json
{
  "type" : "record",
  "name" : "record",
  "fields" : [ {
    "name" : "stringField",
    "type" : "string"
  }, {
    "name" : "stringField2",
    "type" : [ "string", "null" ],
    "default" : "default-for-nullable"
  } ],
  "x-darwin-subject" : "subject-string"
}
```

## Configuration

```hocon
darwin {
  type = "lazy"
  connector = "confluent"

  endpoints: ["http://schema-registry-00:7777", "http://schema-registry-01:7777"]
  max-cached-schemas: 1000
  kafka.schemaregistry.standard-property-1: 1
  kafka.schemaregistry.standard-property-2: "default" 
}

```

The confluent connector can be used by declaring `confluent` as connector.

The `endpoints` configuration is a list of url to the confluent schema registry

the `max-cached-schemas` configures how many schemas are internally cached by the confluent schema registry connector

all other properties will be injected in the confluent schema registry client configuration.

For example if confluent schema registry declares a property `kafka.schemaregistry.auth` this property can simply be
added to the darwin configuration like this

```hocon
darwin {
  type = "lazy"
  connector = "confluent"

  endpoints: ["http://schema-registry-00:7777", "http://schema-registry-01:7777"]
  max-cached-schemas: 1000
  kafka.schemaregistry.auth: "true"
}
```

## Mock

MockConnector can be conveniently used during tests or if all the schemas (past and current) are known when launching 
the application.
The MockConnector can load schemas from local files and classpath resources, it can run in two modes: strict and permissive.
Strict mode fails if any schema cannot be read, while Permissive one will just warn on non-readable schemas. 
Default mode is "strict".

Here is an example of configuration:

```json
"files": ["/home/user/schema1.avsc", "/home/user/schema2.avsc"]
"resources": ["schemas/Apple.avsc", "schemas/Orange.avsc"]
"mode": "permissive"
```

----

## Multi-Connector

Multi-connector can connect to multiple connectors in a hierarchical order. It is useful when schemas are registered on different datastore (i.e. confluent + hbase).

You configure it in the following way:

```
darwin {
  type = "lazy"
  connector = "multi"
  registrar = "hbase"
  confluent-single-object-encoding: "confluent"
  standard-single-object-encoding: ["hbase", "mongo"]
  confluent {
    endpoints: ["http://schema-registry-00:7777", "http://schema-registry-01:7777"]
    max-cached-schemas: 1000
  }
  hbase {
    isSecure: false
    namespace: "DARWIN"
    table: "REPOSITORY"
    coreSite: "/etc/hadoop/conf/core-site.xml"
    hbaseSite: "/etc/hadoop/conf/hbase-site.xml"
  }
  mongo {
    username = "mongo"
    password = "mongo"
    host = ["localhost:12345"]
    database = "test"
    collection = "collection_test"
    timeout = 5000
  }
}
```

When extracting the schemaId, it will check if the single object encoding is "confluent" or "standard" way and extract the id.
Given the id, it will go through the chain of connectors to find the schema: first confluent-single-object-encoding then
standard-single-object-encoding **in order**.
The first that matches, is the one that will be used.

In order to initialize the single connectors, a configuration will be created merging the specific part
(i.e. hbase/mongo/confluent) with the outer layer: in case of duplicated entries the more specific one will be used.

Registration of the schema, will work with the connector set as registrar.

### Notes on Avro Single-Object Encoding Specification

#### Canonical form ignores default fields

As specified in the background section, Darwin leverages the Avro Single-Object Encoding specification to allow the schema fingerprint to be stored along the avro data.
In order to create the fingerprint, the schema is converted into its [parsing-canonical form](https://avro.apache.org/docs/1.8.1/spec.html#Transforming+into+Parsing+Canonical+Form), which strips away all the fields that are not needed for reading/writing, such as doc, alias, comment. However, it will also remove the default field, allowing two schemas that are semantically different due to a default field to have the identical fingerprint.
The default field is important for compatibility; it is useful to know this tip for debugging purposes in case of a broken compatibility on a subject.

Sources:

- [AVRO-2002](https://issues.apache.org/jira/browse/AVRO-2002)
- [Adding or removing a default does not register a new schema version](https://github.com/salsify/avro-schema-registry/issues/38)
