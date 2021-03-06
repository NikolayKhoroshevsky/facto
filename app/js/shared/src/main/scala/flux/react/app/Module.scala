package flux.react.app

import common.I18n
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.Dispatcher
import flux.react.app.balancecheckform.BalanceCheckForm
import flux.react.app.transactiongroupform.TransactionGroupForm
import flux.stores._
import flux.stores.entries.factories._
import models.access.JsEntityAccess
import models.accounting.config.Config
import models.user.User

final class Module(implicit i18n: I18n,
                   accountingConfig: Config,
                   user: User,
                   entityAccess: JsEntityAccess,
                   exchangeRateManager: ExchangeRateManager,
                   allEntriesStoreFactory: AllEntriesStoreFactory,
                   cashFlowEntriesStoreFactory: CashFlowEntriesStoreFactory,
                   liquidationEntriesStoreFactory: LiquidationEntriesStoreFactory,
                   endowmentEntriesStoreFactory: EndowmentEntriesStoreFactory,
                   tagsStoreFactory: TagsStoreFactory,
                   complexQueryStoreFactory: ComplexQueryStoreFactory,
                   summaryForYearStoreFactory: SummaryForYearStoreFactory,
                   summaryYearsStoreFactory: SummaryYearsStoreFactory,
                   summaryExchangeRateGainsStoreFactory: SummaryExchangeRateGainsStoreFactory,
                   globalMessagesStore: GlobalMessagesStore,
                   pageLoadingStateStore: PageLoadingStateStore,
                   pendingModificationsStore: PendingModificationsStore,
                   applicationIsOnlineStore: ApplicationIsOnlineStore,
                   userStore: UserStore,
                   dispatcher: Dispatcher,
                   clock: Clock) {

  // Configuration of submodules
  private val userManagementModule = new flux.react.app.usermanagement.Module
  private val transactionGroupFormModule = new flux.react.app.transactiongroupform.Module
  private val balanceCheckFormModule = new flux.react.app.balancecheckform.Module
  private val transactionViewsModule = new flux.react.app.transactionviews.Module

  implicit private lazy val menu: Menu = new Menu
  implicit private lazy val globalMessages: GlobalMessages = new GlobalMessages
  implicit private lazy val pageLoadingSpinner: PageLoadingSpinner = new PageLoadingSpinner
  implicit private lazy val applicationDisconnectedIcon: ApplicationDisconnectedIcon =
    new ApplicationDisconnectedIcon
  implicit private lazy val pendingModificationsCounter: PendingModificationsCounter =
    new PendingModificationsCounter

  implicit lazy val layout: Layout = new Layout

  implicit lazy val userProfile = userManagementModule.userProfile
  implicit lazy val userAdministration = userManagementModule.userAdministration

  implicit lazy val templateList = new TemplateList

  implicit lazy val transactionGroupForm: TransactionGroupForm =
    transactionGroupFormModule.transactionGroupForm
  implicit lazy val balanceCheckForm: BalanceCheckForm = balanceCheckFormModule.balanceCheckForm

  implicit lazy val everything = transactionViewsModule.everything
  implicit lazy val cashFlow = transactionViewsModule.cashFlow
  implicit lazy val liquidation = transactionViewsModule.liquidation
  implicit lazy val endowments = transactionViewsModule.endowments
  implicit lazy val searchResults = transactionViewsModule.searchResults
  implicit lazy val summary = transactionViewsModule.summary

}
