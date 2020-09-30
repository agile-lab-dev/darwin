package it.agilelab.darwin.common

import it.agilelab.darwin.common.DarwinConcurrentHashMap.{ DarwinJava8ConcurrentHashMap, DarwinTrieConcurrentHashMap }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll }

sealed private[common] class DarwinConcurrentHashMapRunner[K, V](sut: () => DarwinConcurrentHashMap[K, V])
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfter {

  protected class DefaultException extends Exception("Side effect evaluated!")

  protected def anEmptySut: DarwinConcurrentHashMap[K, V] = sut()

}

abstract class DarwinJava8ConcurrentHashMapRunner[K, V]
    extends DarwinConcurrentHashMapRunner[K, V](() => new DarwinJava8ConcurrentHashMap)
abstract class DarwinJava7ConcurrentHashMapRunner[K, V]
    extends DarwinConcurrentHashMapRunner[K, V](() => new DarwinTrieConcurrentHashMap)
