package models.accounting

import models.manager.EntityType

import scala.collection.immutable.Seq
import common.time.LocalDateTime
import models.accounting.money.{ExchangeRateManager, Money, ReferenceMoney}
import models.accounting.config.Config
import models.manager.{Entity, EntityManager}
import models.EntityAccess

/** Transaction groups should be treated as immutable. */
case class TransactionGroup(createdDate: LocalDateTime, idOption: Option[Long] = None) extends Entity {

  override def withId(id: Long) = copy(idOption = Some(id))

  def transactions(implicit entityAccess: EntityAccess): Seq[Transaction] =
    entityAccess.transactionManager.findByGroupId(id)

  def isZeroSum(implicit exchangeRateManager: ExchangeRateManager,
                accountingConfig: Config,
                entityAccess: EntityAccess): Boolean =
    transactions.map(_.flow.exchangedForReferenceCurrency).sum == ReferenceMoney(0)
}

object TransactionGroup {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[TransactionGroup]

  /**
    * Same as TransactionGroup, except all fields are optional, plus an additional `transactions` and `zeroSum` which
    * a `TransactionGroup` stores implicitly.
    */
  case class Partial(transactions: Seq[Transaction.Partial],
                     zeroSum: Boolean = false,
                     createdDate: Option[LocalDateTime] = None,
                     idOption: Option[Long] = None)
  object Partial {
    def from(transactionGroup: TransactionGroup)(implicit entityAccess: EntityAccess,
                                                 accountingConfig: Config,
                                                 exchangeRateManager: ExchangeRateManager): Partial =
      Partial(
        transactions = transactionGroup.transactions.map(Transaction.Partial.from(_)),
        zeroSum = transactionGroup.isZeroSum,
        createdDate = Some(transactionGroup.createdDate),
        idOption = transactionGroup.idOption
      )
  }
}
