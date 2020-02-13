package it.agilelab.darwin.common

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class DarwinConcurrentHashMapSpec extends FlatSpec with Matchers with BeforeAndAfter {
  private val realJavaVersion = System.getProperty("java.version")

  after {
    System.setProperty("java.version", realJavaVersion)
  }

  def test(): Unit = {
    val threadNumber = 1000
    val map = DarwinConcurrentHashMap.empty[String, Int]
    var counter = 0
    val threadCounter = new AtomicInteger(0)
    val runnables = for (_ <- 1 to threadNumber) yield {
      new Runnable {
        override def run(): Unit = {
          threadCounter.incrementAndGet()
          val res = map.getOrElseUpdate("A", {
            counter += 1
            counter
          })
          res should be(1)
        }
      }
    }
    val threads = for (r <- runnables) yield {
      val t = new Thread(r)
      t
    }
    for (t <- threads) {
      t.start()
    }
    for (t <- threads) {
      t.join()
    }
    threadCounter.get() should be(threadNumber)
  }


  it should "not evaluate the value if the key is present JAVA 8" in {
    test()
  }

  it should "not evaluate the value if the key is present JAVA 7" in {
    if (JavaVersion.parseJavaVersion(realJavaVersion) >= 8) {
      System.setProperty("java.version", "1.7")
      test()
    } else {
      assert(true)
    }
  }

  it should "lazy evaluate the default param when using Java8 - getOrElse" in {
    System.setProperty("java.version", "1.8")
    val sut = DarwinConcurrentHashMap.empty[String, Int]
    def defaultWithSideEffect: Int = throw new RuntimeException("Side effect evaluated!")
    val aKey = "aKey"
    val aValue = 1

    sut.getOrElseUpdate(aKey, aValue)

    lazy val res = sut.getOrElse(aKey, defaultWithSideEffect)

    noException should be thrownBy res
    res shouldBe aValue
  }


}
