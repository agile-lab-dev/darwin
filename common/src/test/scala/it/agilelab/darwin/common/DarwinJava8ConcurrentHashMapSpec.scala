package it.agilelab.darwin.common

class DarwinJava8ConcurrentHashMapSpec extends DarwinJava8ConcurrentHashMapRunner {

  private def defaultWithSideEffect: Int = throw new DefaultException
  private val aKey = "aKey"
  private val aValue = 1

  it should "not evaluate the default param when key found - getOrElse" in {
    val sut = DarwinConcurrentHashMap.empty[String, Int]

    def defaultWithSideEffect: Int = throw new DefaultException

    sut.getOrElseUpdate(aKey, aValue)

    lazy val res = sut.getOrElse(aKey, defaultWithSideEffect)

    noException should be thrownBy res
    res shouldBe aValue
  }

  it should "evaluate the default param when key NOT found - getOrElse" in {
    val sut = DarwinConcurrentHashMap.empty[String, Int]

    sut.getOrElseUpdate("aKey", aValue)

    lazy val res = sut.getOrElse("anotherKey", defaultWithSideEffect)

    an[DefaultException] should be thrownBy res
  }

  it should "not evaluate the default param when key is null - getOrElse" in {
    val sut = DarwinConcurrentHashMap.empty[String, Int]

    lazy val res = sut.getOrElse(null, defaultWithSideEffect)

    an[NullPointerException] should be thrownBy res
  }


}
