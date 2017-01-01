package scala2js

import java.time.Month.AUGUST
import java.time.{LocalDate, LocalTime}

import models.manager.EntityType

import scala.scalajs.js
import models.accounting.Transaction
import common.time.LocalDateTime
import models.manager.Entity

import scala.collection.immutable.Seq
import scala2js.Scala2Js.Converter
import js.JSConverters._
import scala.collection.mutable

object Converters {

  // **************** Non-implicits **************** //
  implicit def entityTypeToConverter[E <: Entity : EntityType]: Scala2Js.MapConverter[E] = {
    val entityType: EntityType[E] = implicitly[EntityType[E]]
    val converter: Scala2Js.MapConverter[_ <: Entity] = entityType match {
      case EntityType.TransactionType => TransactionConverter
      case _ => ???
    }
    converter.asInstanceOf[Scala2Js.MapConverter[E]]
  }

  // **************** General converters **************** //
  implicit object NullConverter extends Scala2Js.Converter[js.Any] {
    override def toJs(obj: js.Any) = obj
    override def toScala(obj: js.Any) = obj
  }

  implicit object StringConverter extends Scala2Js.Converter[String] {
    override def toJs(string: String) = string
    override def toScala(value: js.Any) = value.asInstanceOf[String]
  }

  implicit def seqConverter[A](implicit elemConverter: Scala2Js.Converter[A]): Scala2Js.Converter[Seq[A]] =
    new Converter[Seq[A]] {
      override def toJs(seq: Seq[A]) =
        seq.toStream.map(elemConverter.toJs).toJSArray
      override def toScala(value: js.Any) =
        value.asInstanceOf[js.Array[js.Any]].toStream.map(elemConverter.toScala).toVector
    }

  implicit object LocalDateTimeConverter extends Scala2Js.Converter[LocalDateTime] {

    private val secondsInDay = 60 * 60 * 24

    override def toJs(dateTime: LocalDateTime) = {
      val epochDay = dateTime.toLocalDate.toEpochDay.toInt
      val secondOfDay = dateTime.toLocalTime.toSecondOfDay
      epochDay * secondsInDay + secondOfDay
    }
    override def toScala(value: js.Any) = {
      val combinedInt = value.asInstanceOf[Int]
      val epochDay = combinedInt / secondsInDay
      val secondOfDay = combinedInt % secondsInDay
      LocalDateTime.of(
        LocalDate.ofEpochDay(epochDay),
        LocalTime.ofNanoOfDay(secondOfDay))
    }
  }

  // **************** Entity converters **************** //
  implicit object TransactionConverter extends Scala2Js.MapConverter[Transaction] {
    override def toJs(transaction: Transaction) = {
      val dateTimeConverter = implicitly[Scala2Js.Converter[LocalDateTime]]
      val result = js.Dictionary[js.Any](
        "transactionGroupId" -> transaction.transactionGroupId.toString,
        "issuerId" -> transaction.issuerId.toString,
        "beneficiaryAccountCode" -> transaction.beneficiaryAccountCode,
        "moneyReservoirCode" -> transaction.moneyReservoirCode,
        "categoryCode" -> transaction.categoryCode,
        "description" -> transaction.description,
        "flowInCents" -> transaction.flowInCents.toString,
        "detailDescription" -> transaction.detailDescription,
        "tagsString" -> transaction.tagsString,
        "createdDate" -> dateTimeConverter.toJs(transaction.createdDate),
        "transactionDate" -> dateTimeConverter.toJs(transaction.transactionDate),
        "consumedDate" -> dateTimeConverter.toJs(transaction.consumedDate)
      )
      for (id <- transaction.idOption) {
        result.update("id", id.toString)
      }
      result
    }
    override def toScala(dict: js.Dictionary[js.Any]) = {
      def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)
      def getOptional[T: Scala2Js.Converter](key: String) = getOptionalValueFromDict[T](dict)(key)

      Transaction(
        transactionGroupId = getRequired[String]("transactionGroupId").toLong,
        issuerId = getRequired[String]("issuerId").toLong,
        beneficiaryAccountCode = getRequired[String]("beneficiaryAccountCode"),
        moneyReservoirCode = getRequired[String]("moneyReservoirCode"),
        categoryCode = getRequired[String]("categoryCode"),
        description = getRequired[String]("description"),
        flowInCents = getRequired[String]("flowInCents").toLong,
        detailDescription = getRequired[String]("detailDescription"),
        tagsString = getRequired[String]("tagsString"),
        createdDate = getRequired[LocalDateTime]("createdDate"),
        transactionDate = getRequired[LocalDateTime]("transactionDate"),
        consumedDate = getRequired[LocalDateTime]("consumedDate"),
        idOption = getOptional[String]("id").map(_.toLong))
    }
  }
}
