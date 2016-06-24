package controllers.helpers.accounting

import collection.immutable.Seq
import scala.collection.JavaConverters._
import com.google.common.base.Joiner
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import com.google.common.hash.Hashing
import common.cache.UniquelyHashable
import common.cache.UniquelyHashable.UniquelyHashableIterableFunnel
import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.accounting.{Transaction, Transactions}
import models.accounting.config.{Account, Category, Config, MoneyReservoir}
import controllers.helpers.ControllerHelperCache
import controllers.helpers.ControllerHelperCache.CacheIdentifier

case class GeneralEntry(override val transactions: Seq[Transaction])
  extends GroupedTransactions(transactions) with UniquelyHashable {

  override val uniqueHash = {
    Hashing.sha1().newHasher()
      .putObject(transactions, UniquelyHashableIterableFunnel)
      .hash()
  }
}

object GeneralEntry {

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEntries(n: Int): Seq[GeneralEntry] = {
    val transactions: Seq[Transaction] =
      dbRun(
        Transactions.newQuery
          .sortBy(r => (r.transactionDate.desc, r.createdDate.desc))
          .take(3 * n))
        .reverse
        .toList

    var entries = transactions.map(t => GeneralEntry(Seq(t)))

    entries = combineConsecutiveOfSameGroup(entries)

    entries.takeRight(n)
  }

  /* Returns most recent n entries sorted from old to new. */
  def fetchLastNEndowments(account: Account, n: Int): Seq[GeneralEntry] =
    ControllerHelperCache.cached(FetchLastNEndowments(account, n)) {
      val transactions: Seq[Transaction] =
        dbRun(
          Transactions.newQuery
            .filter(_.categoryCode === Config.constants.endowmentCategory.code)
            .filter(_.beneficiaryAccountCode === account.code)
            .sortBy(r => (r.consumedDate.desc, r.createdDate.desc))
            .take(3 * n))
          .reverse
          .toList

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = combineConsecutiveOfSameGroup(entries)

      entries.takeRight(n)
    }

  private[accounting] def combineConsecutiveOfSameGroup(entries: Seq[GeneralEntry]): Seq[GeneralEntry] = {
    GroupedTransactions.combineConsecutiveOfSameGroup(entries) {
      /* combine */ (first, last) => GeneralEntry(first.transactions ++ last.transactions)
    }
  }

  private case class FetchLastNEndowments(account: Account, n: Int) extends CacheIdentifier[Seq[GeneralEntry]] {
    protected override def invalidateWhenUpdating = {
      case transaction: Transaction =>
        transaction.categoryCode == Config.constants.endowmentCategory.code &&
          transaction.beneficiaryAccountCode == account.code
    }
  }
}
