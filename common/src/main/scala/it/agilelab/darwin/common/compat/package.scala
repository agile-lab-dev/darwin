package it.agilelab.darwin.common

/**
  * Converters java <-> scala that works between 2.10, 2.11, 2.12, 2.13
  */
package object compat {
  def toScala[A](jIterable: java.lang.Iterable[A]): scala.collection.Iterable[A] = {
    new Iterable[A] {
      def iterator: scala.collection.Iterator[A] = toScala(jIterable.iterator())
    }
  }

  def toScala[A](jIterator: java.util.Iterator[A]): scala.collection.Iterator[A] = {
    new scala.collection.Iterator[A] {
      def next() = jIterator.next()
      def hasNext = jIterator.hasNext()
    }
  }

  def toScala[A](jSet: java.util.Set[A]): scala.collection.Set[A] = {
    val iterator = jSet.iterator()
    val builder = Set.newBuilder[A]
    while (iterator.hasNext) {
      builder += iterator.next()
    }
    builder.result()
  }

  def toJava[A](iterable: scala.collection.Iterable[A]): java.lang.Iterable[A] = {
    iterable.foldLeft(new java.util.ArrayList[A]()) { (z, x) =>
      z.add(x)
      z
    }
  }

  implicit class IterableConverter[A](jIterable: java.lang.Iterable[A]) {
    def toScala(): scala.collection.Iterable[A] = {
      compat.toScala(jIterable)
    }
  }

  implicit class SetConverter[A](jSet: java.util.Set[A]) {
    def toScala(): scala.collection.Set[A] = {
      compat.toScala(jSet)
    }
  }


  implicit class JIterableConverter[A](iterable: scala.collection.Iterable[A]) {
    def toJava(): java.lang.Iterable[A] = {
      compat.toJava(iterable)
    }
  }

  implicit class IteratorConverter[A](jIterator: java.util.Iterator[A]) {
    def toScala(): scala.collection.Iterator[A] = {
      compat.toScala(jIterator)
    }
  }

  implicit class RightBiasedEither[+L, +R](val self: Either[L, R]) extends AnyVal {
    def rightMap[R1](f: R => R1): Either[L, R1] = {
      self match {
        case Right(v) => Right(f(v))
        case _        => self.asInstanceOf[Either[L, R1]]
      }
    }
  }
}
