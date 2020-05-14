package it.agilelab.darwin.app.mock.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class MyClass(override val value: Int, otherVale: Long) extends MyTrait
