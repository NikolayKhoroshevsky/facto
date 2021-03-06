package models.slick

import java.nio.ByteBuffer

import api.Picklers._
import boopickle.Default.{Pickle, Unpickle}
import common.accounting.Tags
import common.time.LocalDateTime
import models.Entity
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.modification.{EntityModification, EntityModificationEntity}
import models.money.ExchangeRateMeasurement
import models.slick.SlickUtils.dbApi.{Table => SlickTable, Tag => SlickTag, _}
import models.slick.SlickUtils.localDateTimeToSqlDateMapper
import models.user.User

import scala.collection.immutable.Seq

sealed trait SlickEntityTableDef[E <: Entity] {
  type Table <: SlickEntityTableDef.EntityTable[E]
  def tableName: String
  def table(tag: SlickTag): Table
}

object SlickEntityTableDef {

  val all: Seq[SlickEntityTableDef[_]] =
    Seq(
      UserDef,
      TransactionDef,
      TransactionGroupDef,
      BalanceCheckDef,
      ExchangeRateMeasurementDef,
      EntityModificationEntityDef
    )

  /** Table extension to be used with an Entity model. */
  // Based on active-slick (https://github.com/strongtyped/active-slick)
  sealed abstract class EntityTable[E <: Entity](
      tag: SlickTag,
      tableName: String,
      schemaName: Option[String] = None)(implicit val colType: BaseColumnType[Long])
      extends SlickTable[E](tag, schemaName, tableName) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  }

  implicit object UserDef extends SlickEntityTableDef[User] {

    override val tableName: String = "USERS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[User](tag, tableName) {
      def loginName = column[String]("loginName")
      def passwordHash = column[String]("passwordHash")
      def name = column[String]("name")
      def isAdmin = column[Boolean]("isAdmin")
      def expandCashFlowTablesByDefault = column[Boolean]("expandCashFlowTablesByDefault")
      def expandLiquidationTablesByDefault = column[Boolean]("expandLiquidationTablesByDefault")

      override def * =
        (
          loginName,
          passwordHash,
          name,
          isAdmin,
          expandCashFlowTablesByDefault,
          expandLiquidationTablesByDefault,
          id.?) <> (User.tupled, User.unapply)
    }
  }

  implicit object TransactionDef extends SlickEntityTableDef[Transaction] {

    override val tableName: String = "TRANSACTIONS"
    override def table(tag: SlickTag): Table = new Table(tag)

    private implicit val tagsSeqToStringMapper: ColumnType[Seq[String]] = {
      MappedColumnType.base[Seq[String], String](Tags.serializeToString, Tags.parseTagsString)
    }

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[Transaction](tag, tableName) {
      def transactionGroupId = column[Long]("transactionGroupId")
      def issuerId = column[Long]("issuerId")
      def beneficiaryAccountCode = column[String]("beneficiaryAccountCode")
      def moneyReservoirCode = column[String]("moneyReservoirCode")
      def categoryCode = column[String]("categoryCode")
      def description = column[String]("description")
      def flow = column[Long]("flow")
      def detailDescription = column[String]("detailDescription")
      def tags = column[Seq[String]]("tagsString")(tagsSeqToStringMapper)
      def createdDate = column[LocalDateTime]("createdDate")
      def transactionDate = column[LocalDateTime]("transactionDate")
      def consumedDate = column[LocalDateTime]("consumedDate")

      override def * =
        (
          transactionGroupId,
          issuerId,
          beneficiaryAccountCode,
          moneyReservoirCode,
          categoryCode,
          description,
          flow,
          detailDescription,
          tags,
          createdDate,
          transactionDate,
          consumedDate,
          id.?) <> (Transaction.tupled, Transaction.unapply)
    }
  }

  implicit object TransactionGroupDef extends SlickEntityTableDef[TransactionGroup] {

    override val tableName = "TRANSACTION_GROUPS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[TransactionGroup](tag, tableName) {
      def createdDate = column[LocalDateTime]("createdDate")

      override def * = (createdDate, id.?) <> (TransactionGroup.tupled, TransactionGroup.unapply)
    }
  }

  implicit object BalanceCheckDef extends SlickEntityTableDef[BalanceCheck] {

    override val tableName: String = "BALANCE_CHECKS"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[BalanceCheck](tag, tableName) {
      def issuerId = column[Long]("issuerId")
      def moneyReservoirCode = column[String]("moneyReservoirCode")
      def balance = column[Long]("balance")
      def createdDate = column[LocalDateTime]("createdDate")
      def checkDate = column[LocalDateTime]("checkDate")

      override def * =
        (issuerId, moneyReservoirCode, balance, createdDate, checkDate, id.?) <> (BalanceCheck.tupled, BalanceCheck.unapply)
    }
  }

  implicit object ExchangeRateMeasurementDef extends SlickEntityTableDef[ExchangeRateMeasurement] {

    override val tableName: String = "EXCHANGE_RATE_MEASUREMENT"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[ExchangeRateMeasurement](tag, tableName) {
      def date = column[LocalDateTime]("date")
      def foreignCurrencyCode = column[String]("foreignCurrencyCode")
      def ratioReferenceToForeignCurrency = column[Double]("ratioReferenceToForeignCurrency")

      override def * =
        (date, foreignCurrencyCode, ratioReferenceToForeignCurrency, id.?) <> (ExchangeRateMeasurement.tupled, ExchangeRateMeasurement.unapply)
    }
  }

  implicit object EntityModificationEntityDef extends SlickEntityTableDef[EntityModificationEntity] {

    override val tableName: String = "ENTITY_MODIFICATION_ENTITY"
    override def table(tag: SlickTag): Table = new Table(tag)

    /* override */
    final class Table(tag: SlickTag) extends EntityTable[EntityModificationEntity](tag, tableName) {
      def userId = column[Long]("userId")
      def change = column[EntityModification]("modification")
      def date = column[LocalDateTime]("date")

      override def * =
        (userId, change, date, id.?) <> (EntityModificationEntity.tupled, EntityModificationEntity.unapply)
    }

    implicit val entityModificationToBytesMapper: ColumnType[EntityModification] = {
      def toBytes(modification: EntityModification) = {
        val byteBuffer = Pickle.intoBytes(modification)

        val byteArray = new Array[Byte](byteBuffer.remaining)
        byteBuffer.get(byteArray)
        byteArray
      }
      def toEntityModification(bytes: Array[Byte]) = {
        val byteBuffer = ByteBuffer.wrap(bytes)
        Unpickle[EntityModification].fromBytes(byteBuffer)
      }
      MappedColumnType.base[EntityModification, Array[Byte]](toBytes, toEntityModification)
    }
  }
}
