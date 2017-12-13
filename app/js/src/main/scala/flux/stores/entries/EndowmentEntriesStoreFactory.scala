package flux.stores.entries

import jsfacades.LokiJs
import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.accounting.config.{Account, Config}
import models.accounting.{BalanceCheck, Transaction}

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class EndowmentEntriesStoreFactory(implicit database: RemoteDatabaseProxy, accountingConfig: Config)
    extends EntriesListStoreFactory[GeneralEntry, Account] {

  override protected def createNew(maxNumEntries: Int, account: Account) = new Store {
    override protected def calculateState() = {
      val transactions: Seq[Transaction] =
        database
          .newQuery[Transaction]()
          .filter(Keys.Transaction.categoryCode isEqualTo accountingConfig.constants.endowmentCategory.code)
          .filter(Keys.Transaction.beneficiaryAccountCode isEqualTo account.code)
          .sort(
            LokiJs.Sorting
              .descBy(Keys.Transaction.consumedDate)
              .thenDescBy(Keys.Transaction.createdDate)
              .thenDescBy(Keys.id))
          .limit(3 * maxNumEntries)
          .data()
          .reverse

      var entries = transactions.map(t => GeneralEntry(Seq(t)))

      entries = GeneralEntry.combineConsecutiveOfSameGroup(entries)

      EntriesListStoreFactory.State
        .withImpactingIdsInEntries(entries.takeRight(maxNumEntries), hasMore = entries.size > maxNumEntries)
    }

    override protected def transactionUpsertImpactsState(transaction: Transaction, state: State) =
      transaction.category == accountingConfig.constants.endowmentCategory && transaction.beneficiary == account
    override protected def balanceCheckUpsertImpactsState(balanceCheck: BalanceCheck, state: State) = false
  }

  def get(account: Account, maxNumEntries: Int): Store =
    get(Input(maxNumEntries = maxNumEntries, additionalInput = account))
}