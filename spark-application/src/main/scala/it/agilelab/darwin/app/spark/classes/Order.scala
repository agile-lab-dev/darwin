package it.agilelab.darwin.app.spark.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class Order(entries: Seq[(MenuItem, Int)], table: String)
