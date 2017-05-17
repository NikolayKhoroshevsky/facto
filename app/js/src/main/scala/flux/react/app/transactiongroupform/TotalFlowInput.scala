package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import common.time.Clock
import flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.accounting.money._

private[transactiongroupform] final class TotalFlowInput(implicit i18n: I18n,
                                                         clock: Clock,
                                                         exchangeRateManager: ExchangeRateManager) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState_P(props => logExceptions {
      State(valueString = props.forceValue match {
        case Some(forceValue) => forceValue.formatFloat
        case None => props.defaultValue.formatFloat
      })
    })
    .renderPS(($, props, state) => logExceptions {
      def onChange(e: ReactEventI): Callback = LogExceptionsCallback {
        val newString = e.target.value
        val newValue = ReferenceMoney(Money.floatStringToCents(newString) getOrElse 0)
        val oldValue = state.parsedValueOrDefault
        if (oldValue != newValue) {
          props.onChangeListener(newValue)
        }
        $.modState(_.copy(valueString = newString)).runNow()
      }

      val foreignMoney = props.foreignCurrency map { foreignCurrency =>
        val datedMoney = state.parsedValueOrDefault.withDate(clock.now)
        datedMoney.exchangedForCurrency(foreignCurrency)
      }

      <.span(
        ^.className := "total-flow-form form-inline",
        <.div(^.className := "input-group",
          <.span(
            ^.className := "input-group-addon",
            i18n("facto.total") + ":"
          ),
          <.span(
            ^.className := "input-group-addon",
            <.i(^.className := Currency.default.iconClass)
          ),
          <.input(
            ^.tpe := "text",
            ^.className := "form-control",
            ^.autoComplete := "off",
            ^.name := getClass.getSimpleName,
            ^.value := state.valueString,
            ^.disabled := props.forceValue.isDefined,
            ^.onChange ==> onChange
          ),
          ^^.ifThen(foreignMoney) { money =>
            <.span(
              ^.className := "input-group-addon",
              <.i(^.className := money.currency.iconClass),
              <.span(" " + money.formatFloat)
            )
          }
        )
      )
    })
    .componentWillReceiveProps(scope => LogExceptionsCallback {
      // If the props have changed, the transformed value may have changed. If this happens, the listeners should
      // be notified.
      val currentValue = scope.currentState.parsedValueOrDefault
      val newValue = {
        scope.nextProps.forceValue match {
          case Some(forceValue) => forceValue
          case None => currentValue
        }
      }
      if (currentValue != newValue) {
        scope.$.modState(_.copy(valueString = newValue.formatFloat)).runNow()
        scope.nextProps.onChangeListener(newValue)
      }
    })
    .build


  // **************** API ****************//
  def apply(defaultValue: ReferenceMoney = ReferenceMoney(0),
            forceValue: Option[ReferenceMoney],
            foreignCurrency: Option[Currency],
            onChange: ReferenceMoney => Unit): ReactElement = {
    component(Props(
      defaultValue,
      forceValue,
      foreignCurrency,
      onChange))
  }

  // **************** Private inner types ****************//
  case class Props(defaultValue: ReferenceMoney,
                   forceValue: Option[ReferenceMoney],
                   foreignCurrency: Option[Currency],
                   onChangeListener: ReferenceMoney => Unit)

  case class State(valueString: String) {
    def parsedValueOrDefault: ReferenceMoney = ReferenceMoney(Money.floatStringToCents(valueString) getOrElse 0)
  }
}
