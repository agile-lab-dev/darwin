package it.agilelab.darwin.connector.hbase

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{ Admin, ColumnFamilyDescriptorBuilder, TableDescriptorBuilder }

object HBaseUtils {
  def createTable(admin: Admin, tableName: TableName, columnFamily: Array[Byte]): Unit =
    admin.createTable(
      TableDescriptorBuilder
        .newBuilder(tableName)
        .setColumnFamily(
          ColumnFamilyDescriptorBuilder.newBuilder(columnFamily).build()
        )
        .build()
    )
}
