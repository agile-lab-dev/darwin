package it.agilelab.darwin.app.mock.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
abstract class MyNestedAbstractClass[T <: MyTrait](id: Int, myClass: T)
