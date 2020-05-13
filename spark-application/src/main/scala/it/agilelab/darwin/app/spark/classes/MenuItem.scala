package it.agilelab.darwin.app.spark.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class MenuItem(name: String, price: Price, components: Seq[Food])
