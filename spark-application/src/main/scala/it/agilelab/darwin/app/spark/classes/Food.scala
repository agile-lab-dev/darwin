package it.agilelab.darwin.app.spark.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class Food(name: String, allergen: Boolean)
