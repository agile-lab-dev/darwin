package it.agilelab.darwin.manager.util

import java.io.OutputStream
import java.nio.ByteBuffer

import it.agilelab.darwin.common.LONG_SIZE


private[darwin] object ByteArrayUtils {


  implicit class EnrichedLong(val l: Long) extends AnyVal {
    /** Converts Long to Array[Byte].
      */
    def longToByteArray: Array[Byte] = {
      ByteBuffer.allocate(LONG_SIZE).putLong(l).array()
    }

    def writeToStream(os: OutputStream): Unit = {
      os.write((l >>> 56).asInstanceOf[Int])
      os.write((l >>> 48).asInstanceOf[Int])
      os.write((l >>> 40).asInstanceOf[Int])
      os.write((l >>> 32).asInstanceOf[Int])
      os.write((l >>> 24).asInstanceOf[Int])
      os.write((l >>> 16).asInstanceOf[Int])
      os.write((l >>> 8).asInstanceOf[Int])
      os.write((l >>> 0).asInstanceOf[Int])
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
