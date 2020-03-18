package it.agilelab.darwin.common

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

private[common] abstract class DarwinConcurrentHashMapRunner(version: String) extends FlatSpec with Matchers with BeforeAndAfterAll {

  protected class DefaultException extends Exception("Side effect evaluated!")

  override protected def beforeAll(): Unit =
    System.setProperty("java.version", version)

}

class DarwinJava8ConcurrentHashMapRunner extends DarwinConcurrentHashMapRunner("1.8")
