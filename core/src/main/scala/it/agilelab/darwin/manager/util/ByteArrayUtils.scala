package it.agilelab.darwin.manager.util

import java.nio.ByteBuffer

private[darwin] object ByteArrayUtils {

  implicit class EnrichedLong(l: Long) {
    /** Converts Long to Array[Byte].
      */
    def longToByteArray: Array[Byte] = {
      (0 to 7).foldLeft(Array.empty[Byte])((z, idx) => z :+ ((l >> ((7 - idx) * 8)) & 0xff).toByte)
    }
  }

  implicit class EnrichedByteArray(a: Array[Byte]) {
    /** Converts Array[Byte] to Long.
      * throws java.nio.BufferUnderflowException if array size isn't 8. (Long require 64 bit)
      */
    def byteArrayToLong: Long = ByteBuffer.wrap(a).getLong
  }
}
