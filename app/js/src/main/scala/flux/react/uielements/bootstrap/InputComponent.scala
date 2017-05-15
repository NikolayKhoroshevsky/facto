package flux.react.uielements.bootstrap

import java.util.NoSuchElementException

import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.uielements.InputBase
import common.LoggingUtils
import japgolly.scalajs.react.{ReactEventI, TopNode, _}
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import japgolly.scalajs.react.ReactComponentC.ReqProps
import org.scalajs.dom.raw.HTMLInputElement

import scala.collection.immutable.Seq
import scala.util.{Success, Failure, Try}

private[bootstrap] object InputComponent {

  // **************** API ****************//
  def create[Value, ExtraProps](name: String,
                                valueChangeForPropsChange: (Props[Value, ExtraProps], Value) => Value = (_: Props[Value, ExtraProps], oldValue: Value) => oldValue,
                                inputRenderer: InputRenderer[ExtraProps]) = {
    def calculateInitialState(props: Props[Value, ExtraProps]): State[Value] = logExceptions {
      // Calling valueChangeForPropsChange() to make sure there is no discrepancy between init and update.
      val value = valueChangeForPropsChange(props, props.defaultValue)
      State(
        valueString = ValueTransformer.valueToString(value, props),
        listeners = Seq(props.listener))
    }
    ReactComponentB[Props[Value, ExtraProps]](name)
      .initialState_P[State[Value]](calculateInitialState)
      .renderPS((context, props, state) => logExceptions {
        def onChange(e: ReactEventI): Callback = LogExceptionsCallback {
          val newString = e.target.value
          val newValue = ValueTransformer.stringToValueOrDefault(newString, props)
          val oldValue = ValueTransformer.stringToValueOrDefault(state.valueString, props)
          if (oldValue != newValue) {
            for (listener <- context.state.listeners) {
              listener.onChange(newValue, directUserChange = true).runNow()
            }
          }
          context.modState(_.withValueString(newString)).runNow()
        }
        val errorMessage = generateErrorMessage(state, props)

        <.div(
          ^^.classes(Seq("form-group") ++ errorMessage.map(_ => "has-error")),
          <.label(
            ^.className := "col-sm-4 control-label",
            props.label),
          <.div(
            ^.className := "col-sm-8",
            inputRenderer.renderInput(
              classes = "form-control" +: props.inputClasses,
              name = props.name,
              valueString = state.valueString,
              onChange = onChange,
              extraProps = props.extra),
            <<.ifThen(errorMessage) { msg =>
              <.span(^.className := "help-block", msg)
            }
          )
        )
      })
      .componentWillReceiveProps(scope => LogExceptionsCallback {
        // If the props have changed, the transformed value may have changed. If this happens, the listeners should
        // be notified.
        val valueString = scope.currentState.valueString
        val currentValue = ValueTransformer.stringToValueOrDefault(valueString, scope.currentProps)
        val newValue = {
          val transformedValue = ValueTransformer.stringToValueOrDefault(valueString, scope.nextProps)
          valueChangeForPropsChange(scope.nextProps, transformedValue)
        }
        if (currentValue != newValue) {
          scope.$.modState(_.withValueString(ValueTransformer.valueToString(newValue, scope.nextProps))).runNow()
          for (listener <- scope.currentState.listeners) {
            listener.onChange(newValue, directUserChange = false).runNow()
          }
        }
      })
      .build
  }

  // **************** Private methods ****************//
  def generateErrorMessage[Value, ExtraProps](state: State[Value], props: Props[Value, ExtraProps]): Option[String] = {
    if (props.showErrorMessage) {
      ValueTransformer.stringToValue(state.valueString, props) match {
        case None => Some("Invalid value")
        case Some(props.defaultValue) if !props.defaultIsValid => Some("Please change this value")
        case _ => None
      }
    } else {
      None
    }
  }

  // **************** Public inner types ****************//
  type ThisRefComp[Value, ExtraProps] = RefComp[Props[Value, ExtraProps], State[Value], Backend, _ <: TopNode]

  trait InputRenderer[ExtraProps] {
    def renderInput(classes: Seq[String],
                    name: String,
                    valueString: String,
                    onChange: ReactEventI => Callback,
                    extraProps: ExtraProps): ReactNode
  }

  trait ValueTransformer[Value, -ExtraProps] {

    /**
      * Returns the Value that corresponds to the given String or None iff the current value is
      * invalid (excluding the case where the default value is invalid).
      */
    def stringToValue(string: String, extraProps: ExtraProps): Option[Value]

    /**
      * Returns the string value of given value.
      *
      * @throws Exception (any) if the given value is invalid for given props
      */
    def valueToString(value: Value, extraProps: ExtraProps): String
  }

  object ValueTransformer {
    def nullInstance[ExtraProps]: ValueTransformer[String, ExtraProps] = new ValueTransformer[String, ExtraProps] {
      override def stringToValue(string: String, extraProps: ExtraProps) = Some(string)
      override def valueToString(value: String, extraProps: ExtraProps) = value
    }

    def stringToValue[Value, ExtraProps](string: String, props: Props[Value, ExtraProps]): Option[Value] = {
      props.valueTransformer.stringToValue(string, props.extra)
    }

    def stringToValueOrDefault[Value, ExtraProps](string: String, props: Props[Value, ExtraProps]): Value = {
      val maybeValue = stringToValue(string, props)
      maybeValue getOrElse props.defaultValue
    }

    def valueToString[Value, ExtraProps](value: Value, props: Props[Value, ExtraProps]): String = {
      props.valueTransformer.valueToString(value, props.extra)
    }
  }

  case class Props[Value, ExtraProps](label: String,
                                      name: String,
                                      defaultValue: Value,
                                      defaultIsValid: Boolean,
                                      showErrorMessage: Boolean,
                                      inputClasses: Seq[String],
                                      listener: InputBase.Listener[Value],
                                      extra: ExtraProps = (): Unit,
                                      valueTransformer: ValueTransformer[Value, ExtraProps])

  case class State[Value](valueString: String,
                          listeners: Seq[InputBase.Listener[Value]] = Seq()) {
    def withValueString(newString: String): State[Value] = copy(valueString = newString)
    def withListener(listener: InputBase.Listener[Value]): State[Value] = copy(listeners = listeners :+ listener)
    def withoutListener(listener: InputBase.Listener[Value]): State[Value] = copy(listeners = listeners.filter(_ != listener))
  }

  abstract class Reference[Value, ExtraProps](refComp: ThisRefComp[Value, ExtraProps]) extends InputBase.Reference[Value] {
    override final def apply($: BackendScope[_, _]): InputBase.Proxy[Value] = new Proxy[Value, ExtraProps](() => refComp($).get)
    override final def name = refComp.name
  }

  // **************** Private inner types ****************//
  private type Backend = Unit
  private type ThisComponentU[Value, ExtraProps] = ReactComponentU[Props[Value, ExtraProps], State[Value], Backend, _ <: TopNode]

  private final class Proxy[Value, ExtraProps](val componentProvider: () => ThisComponentU[Value, ExtraProps]) extends InputBase.Proxy[Value] {
    override def value = {
      ValueTransformer.stringToValue(componentProvider().state.valueString, props) match {
        case Some(value) if value == props.defaultValue && !props.defaultIsValid => None
        case other => other
      }
    }
    override def valueOrDefault = ValueTransformer.stringToValueOrDefault(componentProvider().state.valueString, props)
    override def setValue(newValue: Value) = {
      def setValueInternal(newValue: Value): Value = {
        val stringValue = ValueTransformer.valueToString(newValue, props)
        componentProvider().modState(_.withValueString(stringValue))
        for (listener <- componentProvider().state.listeners) {
          listener.onChange(newValue, directUserChange = false).runNow()
        }
        newValue
      }
      val maybeStringValue = Try(ValueTransformer.valueToString(newValue, props))
      maybeStringValue match {
        case Success(stringValue) =>
          val valueThroughTransformer = ValueTransformer.stringToValueOrDefault(stringValue, props)
          if (newValue == valueThroughTransformer) {
            setValueInternal(newValue)
          } else {
            println(s"  Setting a value ('$newValue') that is different when transformed to string and back to value " +
              s"(valueThroughTransformer = '$valueThroughTransformer'). Will ignore this setter.")
            this.valueOrDefault
          }
        case Failure(_) =>
          println(s"  Failed to get the String value for ${newValue}. This may be intended if the valid options for " +
            s"this input change. Will ignore this setter.")
          this.valueOrDefault
      }
    }
    override def registerListener(listener: InputBase.Listener[Value]) = componentProvider().modState(_.withListener(listener))
    override def deregisterListener(listener: InputBase.Listener[Value]) = {
      try {
        componentProvider().modState(_.withoutListener(listener))
      } catch {
        case _: NoSuchElementException => // Ignore the case this component no longer exists
      }
    }

    private def props: Props[Value, ExtraProps] = componentProvider().props
  }
}
