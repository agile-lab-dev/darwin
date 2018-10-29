package it.agilelab.darwin.app.mock.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
case class MyNestedClass(id: Int, myClass: MyClass, my2Class: Map[String, MyClass])
  extends MyNestedAbstractClass[MyClass](id, myClass)
