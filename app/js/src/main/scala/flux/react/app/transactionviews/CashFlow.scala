package flux.react.app.transactionviews

import common.Formatting._
import common.{I18n, Unique}
import common.LoggingUtils.LogExceptionsCallback
import common.money.ExchangeRateManager
import common.time.Clock
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.<<
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.{Page, RouterContext}
import flux.react.uielements
import flux.stores.entries.{CashFlowEntriesStoreFactory, CashFlowEntry}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomArray
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.BalanceCheck
import models.accounting.config.{Config, MoneyReservoir}
import models.EntityAccess
import models.user.User

import scala.collection.immutable.Seq
import scala.scalajs.js

final class CashFlow(implicit entriesStoreFactory: CashFlowEntriesStoreFactory,
                     dispatcher: Dispatcher,
                     entityAccess: EntityAccess,
                     clock: Clock,
                     accountingConfig: Config,
                     user: User,
                     exchangeRateManager: ExchangeRateManager,
                     i18n: I18n) {

  private val entriesListTable: EntriesListTable[CashFlowEntry, MoneyReservoir] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(
      State(
        includeUnrelatedReservoirs = false,
        includeHiddenReservoirs = false,
        setExpanded = Unique(user.expandCashFlowTablesByDefault)))
    .renderPS(
      ($, props, state) => {
        implicit val router = props.router
        <.span(
          uielements.PageHeader.withExtension(router.currentPage) {
            uielements.CollapseAllExpandAllButtons(setExpanded =>
              $.modState(_.copy(setExpanded = setExpanded)))
          }, {
            for {
              account <- accountingConfig.personallySortedAccounts
              if state.includeUnrelatedReservoirs || account.isMineOrCommon
            } yield {
              uielements.Panel(i18n("facto.account-of", account.longName), key = account.code) {
                {
                  for {
                    reservoir <- accountingConfig
                      .moneyReservoirs(
                        includeNullReservoir = false,
                        includeHidden = state.includeHiddenReservoirs)
                    if reservoir.owner == account
                  } yield {
                    entriesListTable.withRowNumber(
                      tableTitle = reservoir.name,
                      tableClasses = Seq("table-cashflow"),
                      key = reservoir.code,
                      numEntriesStrategy = NumEntriesStrategy(start = 10, intermediateBeforeInf = Seq(30)),
                      setExpanded = state.setExpanded,
                      additionalInput = reservoir,
                      latestEntryToTableTitleExtra =
                        latestEntry => s"${i18n("facto.balance")}: ${latestEntry.balance}",
                      tableHeaders = Seq(
                        <.th(i18n("facto.payed")),
                        <.th(i18n("facto.consumed")),
                        <.th(i18n("facto.beneficiary")),
                        <.th(i18n("facto.category")),
                        <.th(i18n("facto.description")),
                        <.th(i18n("facto.flow")),
                        <.th(i18n("facto.balance")),
                        <.th(balanceCheckAddNewButton(reservoir))
                      ),
                      calculateTableDataFromEntryAndRowNum = (cashFlowEntry, rowNumber) =>
                        cashFlowEntry match {
                          case entry: CashFlowEntry.RegularEntry =>
                            Seq[VdomElement](
                              <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                              <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                              <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                              <.td(entry.categories.map(_.name).mkString(", ")),
                              <.td(uielements.DescriptionWithEntryCount(entry)),
                              <.td(uielements.MoneyWithCurrency(entry.flow)),
                              <.td(
                                uielements.MoneyWithCurrency(entry.balance),
                                entry.balanceVerified match {
                                  case true => <.span(" ", <.i(^.className := "fa fa-check fa-fw"))
                                  case false if rowNumber == 0 =>
                                    <.span(" ", balanceCheckConfirmButton(reservoir, entry))
                                  case _ => VdomArray.empty()
                                }
                              ),
                              <.td(uielements.TransactionGroupEditButton(entry.groupId))
                            )
                          case entry @ CashFlowEntry.BalanceCorrection(balanceCorrection, expectedAmount) =>
                            Seq[VdomElement](
                              <.td(formatDate(balanceCorrection.checkDate)),
                              <.td(
                                ^.colSpan := 4,
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                i18n("facto.balance-correction") + ":"),
                              <.td(
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                if (entry.balanceIncrease.cents > 0) "+" else "-",
                                " ",
                                uielements.MoneyWithCurrency(
                                  if (entry.balanceIncrease.cents > 0) entry.balanceIncrease
                                  else entry.balanceIncrease.negated)
                              ),
                              <.td(
                                ^.style := js.Dictionary("fontWeight" -> "bold"),
                                uielements.MoneyWithCurrency(balanceCorrection.balance)),
                              <.td(balanceCheckEditButton(balanceCorrection))
                            )
                      }
                    )
                  }
                }.toVdomArray
              }
            }
          }.toVdomArray,
          // includeUnrelatedReservoirs toggle button
          <.a(
            ^.className := "btn btn-info btn-lg btn-block",
            ^.onClick --> LogExceptionsCallback(
              $.modState(s => s.copy(includeUnrelatedReservoirs = !s.includeUnrelatedReservoirs)).runNow()),
            if (state.includeUnrelatedReservoirs) i18n("facto.hide-other-accounts")
            else i18n("facto.show-other-accounts")
          ),
          // includeHiddenReservoirs toggle button
          <<.ifThen(state.includeHiddenReservoirs || state.includeUnrelatedReservoirs) {
            <.a(
              ^.className := "btn btn-info btn-lg btn-block",
              ^.onClick --> LogExceptionsCallback(
                $.modState(s => s.copy(includeHiddenReservoirs = !s.includeHiddenReservoirs)).runNow()),
              if (state.includeHiddenReservoirs) i18n("facto.hide-hidden-reservoirs")
              else i18n("facto.show-hidden-reservoirs")
            )
          }
        )
      }
    )
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private helper methods ****************//
  private def balanceCheckAddNewButton(reservoir: MoneyReservoir)(
      implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(Page.NewBalanceCheck(reservoir))(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw"),
      " ",
      i18n("facto.set")
    )
  }

  private def balanceCheckConfirmButton(reservoir: MoneyReservoir, entry: CashFlowEntry.RegularEntry)(
      implicit router: RouterContext): VdomElement = {
    <.button(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      ^.onClick --> LogExceptionsCallback {
        dispatcher.dispatch(
          Action.AddBalanceCheck(BalanceCheck(
            issuerId = user.id,
            moneyReservoirCode = reservoir.code,
            balanceInCents = entry.balance.cents,
            createdDate = clock.now,
            checkDate = entry.mostRecentTransaction.transactionDate
          )))
      }.void,
      <.i(^.className := "fa fa-check-square-o fa-fw")
    )
  }

  def balanceCheckEditButton(balanceCorrection: BalanceCheck)(implicit router: RouterContext): VdomElement = {
    router.anchorWithHrefTo(Page.EditBalanceCheck(balanceCorrection))(
      ^.className := "btn btn-default btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-pencil fa-fw"),
      " ",
      i18n("facto.edit")
    )
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State(includeUnrelatedReservoirs: Boolean,
                           includeHiddenReservoirs: Boolean,
                           setExpanded: Unique[Boolean])
}