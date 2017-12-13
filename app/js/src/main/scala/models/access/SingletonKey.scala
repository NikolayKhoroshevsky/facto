package models.access

import common.ScalaUtils
import common.ScalaUtils.visibleForTesting
import common.time.LocalDateTime

import scala2js.Converters._
import scala2js.Scala2Js

@visibleForTesting
sealed trait SingletonKey[V] {
  implicit def valueConverter: Scala2Js.Converter[V]

  def name: String = ScalaUtils.objectName(this)
  override def toString = name
}

@visibleForTesting
object SingletonKey {
  abstract class StringSingletonKey extends SingletonKey[String] {
    override val valueConverter = implicitly[Scala2Js.Converter[String]]
  }
  abstract class DateTimeSingletonKey extends SingletonKey[LocalDateTime] {
    override val valueConverter = implicitly[Scala2Js.Converter[LocalDateTime]]
  }

  object NextUpdateTokenKey extends DateTimeSingletonKey
  object VersionKey extends StringSingletonKey
}
