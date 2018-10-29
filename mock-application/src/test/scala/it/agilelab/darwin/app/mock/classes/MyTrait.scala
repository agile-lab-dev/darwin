package it.agilelab.darwin.app.mock.classes

import it.agilelab.darwin.annotations.AvroSerde

@AvroSerde
trait MyTrait {
  def value: Int
}
