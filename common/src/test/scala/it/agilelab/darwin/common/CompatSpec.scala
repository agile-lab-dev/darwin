package it.agilelab.darwin.common

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import compat._

class CompatSpec extends AnyFlatSpec with Matchers {

  "RightBiasedEither" should "map correctly on left side" in {
    Left[Int, String](3).rightMap {
      "Hello" + _
    } shouldBe Left[Int, String](3)
  }

  it should "map correctly on right side" in {
    Right[Int, String]("Darwin").rightMap {
      "Hello " + _
    } shouldBe Right[Int, String]("Hello Darwin")
  }
}
