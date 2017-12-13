package common

import java.time.Duration

import scala.collection.immutable.Seq
import scala.collection.mutable

/** Replaces some Guava-provided functionality that is not usable with scala.js. */
object GuavaReplacement {

  // **************** Iterables **************** //
  object Iterables {
    def getOnlyElement[T](traversable: Traversable[T]): T = {
      val list = traversable.toList
      require(list.size == 1, s"Given traversable can only have one element but is $list")
      list.head
    }
  }

  // **************** DoubleMath **************** //
  object DoubleMath {
    def roundToLong(x: Double): Long = {
      Math.round(x)
    }
  }

  // **************** Preconditions **************** //
  object Preconditions {
    def checkNotNull[T](value: T): T = {
      if (value == null) {
        throw new NullPointerException()
      }
      value
    }
  }

  // **************** Splitter **************** //
  final class Splitter(separator: Char) {
    private var _omitEmptyStrings: Boolean = false
    private var _trimResults: Boolean = false

    def omitEmptyStrings(): this.type = {
      _omitEmptyStrings = true
      this
    }

    def trimResults(): this.type = {
      _trimResults = true
      this
    }

    def split(string: String): Seq[String] = {
      val parts = mutable.Buffer[String]()
      val nextPart = new StringBuilder

      for (char <- string) char match {
        case `separator` =>
          parts += nextPart.result()
          nextPart.clear()
        case _ =>
          nextPart += char
      }
      parts += nextPart.result()
      var result = Seq(parts: _*)
      if (_trimResults) {
        result = result.map(_.trim)
      }
      if (_omitEmptyStrings) {
        result = result.filter(_.nonEmpty)
      }
      result
    }
  }
  object Splitter {
    def on(separator: Char): Splitter = new Splitter(separator)
  }

  // **************** Stopwatch **************** //
  final class Stopwatch private () {
    private var startTimeMillis = System.currentTimeMillis

    def elapsed: Duration = {
      val nowMillis = System.currentTimeMillis
      Duration.ofMillis(nowMillis - startTimeMillis)
    }

    def reset(): Unit = {
      startTimeMillis = System.currentTimeMillis
    }
  }

  object Stopwatch {
    def createStarted(): Stopwatch = new Stopwatch()
  }

  // **************** Multimap **************** //
  final class ImmutableSetMultimap[A, B](private val backingMap: Map[A, Set[B]]) {
    def get(key: A): Set[B] = backingMap.getOrElse(key, Set())
    def keySet: Set[A] = backingMap.keySet
    def containsValue(value: B): Boolean = backingMap.values.toStream.flatten.contains(value)
    def values: Iterable[B] = backingMap.values.toStream.flatMap(set => set)

    override def toString = backingMap.toString

    override def equals(that: scala.Any) = that match {
      case that: ImmutableSetMultimap[A, B] => backingMap == that.backingMap
      case _ => false
    }
    override def hashCode() = backingMap.hashCode()
  }
  object ImmutableSetMultimap {
    def builder[A, B](): Builder[A, B] = new Builder[A, B]()
    def of[A, B](): ImmutableSetMultimap[A, B] = new Builder[A, B]().build()

    final class Builder[A, B] private[ImmutableSetMultimap] () {
      private val backingMap = mutable.Map[A, Set[B]]()

      def put(key: A, value: B): Builder[A, B] = {
        val existingList = backingMap.getOrElse(key, Set())
        backingMap.put(key, existingList + value)
        this
      }
      def putAll(key: A, values: B*): Builder[A, B] = {
        val existingList = backingMap.getOrElse(key, Set())
        backingMap.put(key, existingList ++ values)
        this
      }

      def build(): ImmutableSetMultimap[A, B] = new ImmutableSetMultimap(backingMap.toMap)
    }
  }

  // **************** LoadingCache **************** //
  final class LoadingCache[K, V](loader: K => V) {
    private val backingMap: mutable.Map[K, V] = mutable.Map()

    def get(key: K): V = this.synchronized {
      if (backingMap contains key) {
        backingMap(key)
      } else {
        val value = loader(key)
        backingMap.put(key, value)
        value
      }
    }

    def asMap(): Map[K, V] = this.synchronized {
      backingMap.toMap
    }
  }

  object LoadingCache {
    def fromLoader[K, V](loader: K => V): LoadingCache[K, V] = new LoadingCache(loader)
  }
}