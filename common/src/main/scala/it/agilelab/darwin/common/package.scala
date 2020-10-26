package it.agilelab.darwin

package object common {

  def using[A <: AutoCloseable, B](closeable: A)(f: A => B): B = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }

  final val LONG_SIZE = 8
  final val INT_SIZE = 4

}
