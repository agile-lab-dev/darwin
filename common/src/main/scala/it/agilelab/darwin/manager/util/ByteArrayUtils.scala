package it.agilelab.darwin.manager.util

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

import it.agilelab.darwin.common.LONG_SIZE

import java.util.function.{Function => JFunction}

private[darwin] object ByteArrayUtils {

  val longToBytes = new ConcurrentHashMap[Long, Array[Byte]]()

  implicit class EnrichedLong(val l: Long) extends AnyVal {
    /** Converts Long to Array[Byte].
      */
    def longToByteArray: Array[Byte] = {
      longToBytes.computeIfAbsent(l, new JFunction[Long, Array[Byte]] {
        override def apply(t: Long): Array[Byte] = ByteBuffer.allocate(LONG_SIZE).putLong(l).array()
      })
    }
  }

  def arrayEquals(b1: Array[Byte], b2: Array[Byte], start1: Int, start2: Int, length: Int): Boolean = {
    require(length > 0, "length must be positive")
    var i = start1
    var j = start2
    var areEqual = true
    while (areEqual && i < start1 + length) {
      if (b1(i) != b2(j)) {
        areEqual = false
      }
      i += 1
      j += 1
    }
    areEqual
  }


}
