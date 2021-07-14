package it.agilelab.darwin.common

import java.util

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
      def next()  = jIterator.next()
      def hasNext = jIterator.hasNext()
    }
  }

  def toScala[A, B](jIterator: java.util.Map[A, B]): scala.collection.Map[A, B] = {
    toScala(jIterator.entrySet().iterator()).map(x => (x.getKey, x.getValue)).toMap
  }

  def toScala[A](jSet: java.util.Set[A]): scala.collection.Set[A] = {
    val iterator = jSet.iterator()
    val builder  = Set.newBuilder[A]
    while (iterator.hasNext) {
      builder += iterator.next()
    }
    builder.result()
  }

  def toJava[A](iterable: scala.collection.Iterable[A]): java.lang.Iterable[A] = new java.lang.Iterable[A] {
    override def iterator(): util.Iterator[A] = new util.Iterator[A] {
      private val it                = iterable.iterator
      override def hasNext: Boolean = it.hasNext
      override def next(): A        = it.next()
    }
  }

  def toJava[A](list: List[A]): java.util.List[A] = {
    val arraylist = new util.ArrayList[A]()
    list.foreach(arraylist.add)
    arraylist
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

    def toJavaList(): java.util.List[A] = {
      compat.toJava(iterable.toList)
    }
  }

  implicit class JMapConverter[A, B](map: scala.collection.Map[A, B]) {
    def toJava(): java.util.Map[A, B] = {
      val hashmap: util.Map[A, B] = new util.HashMap[A, B]()
      map.foreach { case (k, v) =>
        hashmap.put(k, v)
      }
      hashmap
    }

  }

  implicit class IteratorConverter[A](jIterator: java.util.Iterator[A]) {
    def toScala(): scala.collection.Iterator[A] = {
      compat.toScala(jIterator)
    }
  }

  implicit class MapConverter[A, B](jmap: java.util.Map[A, B]) {
    def toScala(): collection.Map[A, B] = {
      compat.toScala(jmap)
    }
  }

  implicit class RightBiasedEither[+L, +R](val self: Either[L, R]) extends AnyVal {
    def rightMap[R1](f: R => R1): Either[L, R1] = {
      self match {
        case Right(v) => Right(f(v))
        case _        => self.asInstanceOf[Either[L, R1]]
      }
    }

    def rightFlatMap[L1 >: L, R1](f: R => Either[L1, R1]): Either[L1, R1] = {
      self match {
        case Right(v) => f(v)
        case _        => self.asInstanceOf[Either[L1, R1]]
      }
    }
  }
}
