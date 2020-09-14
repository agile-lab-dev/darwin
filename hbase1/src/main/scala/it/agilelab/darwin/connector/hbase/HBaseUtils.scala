package it.agilelab.darwin.connector.hbase

import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.Admin

object HBaseUtils {
  def createTable(admin: Admin, tableName: TableName, columnFamily: Array[Byte]): Unit = {
    admin.createTable(new HTableDescriptor(tableName).addFamily(new HColumnDescriptor(columnFamily)))
  }
}
