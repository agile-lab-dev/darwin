package it.agilelab.darwin.common

import java.util.function.{ Function => JFunction }

import scala.collection.concurrent.TrieMap

/**
  * A thread safe lock-free concurrent map that exposes only getOrElseUpdate and getOrElse methods
  * It is backed by either a scala.collection.concurrent.TrieMap or java.util.concurrent.ConcurrentHashMap
  * depending on the JVM that executes Darwin.
  * JVM 8 or later use java's ConcurrentHashMap while earlier versions use scala's TrieMap
  *
  * Obtain the "correct" instance using {{{DarwinConcurrentHashMap.empty}}} factory method.
  */
trait DarwinConcurrentHashMap[K, V] {
  def getOrElseUpdate(k: K, newValue: => V): V

  def getOrElse(k: K, default: => V): V
}

object DarwinConcurrentHashMap {

  private[common] class DarwinJava8ConcurrentHashMap[K, V] extends DarwinConcurrentHashMap[K, V] {
    private val innerMap = new java.util.concurrent.ConcurrentHashMap[K, V]()

    override def getOrElseUpdate(k: K, newValue: => V): V = {
      innerMap.computeIfAbsent(
        k,
        new JFunction[K, V]() {
          override def apply(t: K): V = newValue
        }
      )
    }

    override def getOrElse(k: K, default: => V): V =
      Option(innerMap.get(k)).getOrElse(default)
  }

  private[common] class DarwinTrieConcurrentHashMap[K, V] extends DarwinConcurrentHashMap[K, V] {
    private val innerMap = TrieMap.empty[K, V]

    override def getOrElseUpdate(k: K, newValue: => V): V = innerMap.getOrElseUpdate(k, newValue)

    override def getOrElse(k: K, default: => V): V = innerMap.getOrElse(k, default)
  }

  private val isJavaAtLeast8 = JavaVersion.current() >= 8

  def empty[K, V]: DarwinConcurrentHashMap[K, V] = {
    if (isJavaAtLeast8) {
      new DarwinJava8ConcurrentHashMap()
    } else {
      new DarwinTrieConcurrentHashMap()
    }
  }
}
