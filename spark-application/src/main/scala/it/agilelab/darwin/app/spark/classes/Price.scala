package it.agilelab.darwin.app.spark.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class Price(amount: Float, discount: Float)
