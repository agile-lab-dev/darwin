package it.agilelab.darwin.common

import it.agilelab.darwin.common.DarwinConcurrentHashMap.DarwinTrieConcurrentHashMap

class DarwinTrieConcurrentHashMapSpec extends DarwinJava7ConcurrentHashMapRunner[String, Int] {

  private def defaultWithSideEffect: Int = throw new DefaultException

  private val aKey   = "aKey"
  private val aValue = 1

  it should "not evaluate the default param when key found - getOrElse" in {
    val sut = anEmptySut

    sut.getOrElseUpdate(aKey, aValue)

    lazy val res = sut.getOrElse(aKey, defaultWithSideEffect)

    sut shouldBe a[DarwinTrieConcurrentHashMap[_, _]]
    noException should be thrownBy res
    res shouldBe aValue
  }

  it should "evaluate the default param when key NOT found - getOrElse" in {
    val sut = anEmptySut
    sut.getOrElseUpdate(aKey, aValue)

    lazy val res = sut.getOrElse("anotherKey", defaultWithSideEffect)

    sut shouldBe a[DarwinTrieConcurrentHashMap[_, _]]
    an[DefaultException] should be thrownBy res
  }

  it should "evaluate the default param when key is null - getOrElse" in {
    val sut = anEmptySut

    lazy val res = sut.getOrElse(null, defaultWithSideEffect)

    sut shouldBe a[DarwinTrieConcurrentHashMap[_, _]]
    an[DefaultException] should be thrownBy res
  }

}
