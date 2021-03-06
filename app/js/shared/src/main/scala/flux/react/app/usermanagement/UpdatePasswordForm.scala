package flux.react.app.usermanagement

import api.ScalaJsApi.UserPrototype
import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.{Action, Dispatcher}
import flux.react.uielements
import flux.react.uielements.input.bootstrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import models.user.User

import scala.collection.immutable.Seq

private[usermanagement] final class UpdatePasswordForm(implicit user: User,
                                                       i18n: I18n,
                                                       dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State())
    .renderBackend[Backend]
    .build

  // **************** API ****************//
  def apply(): VdomElement = {
    component(Props())
  }

  // **************** Private inner types ****************//
  private case class Props()
  private case class State(showErrorMessages: Boolean = false, globalErrors: Seq[String] = Seq())

  private final class Backend(val $ : BackendScope[Props, State]) {

    private val passwordRef = bootstrap.TextInput.ref()
    private val passwordVerificationRef = bootstrap.TextInput.ref()

    def render(props: Props, state: State) = logExceptions {
      <.form(
        ^.className := "form-horizontal",
        uielements.HalfPanel(title = <.span(i18n("app.change-password")))(
          {
            for (error <- state.globalErrors) yield {
              <.div(^.className := "alert alert-danger", ^.key := error, error)
            }
          }.toVdomArray,
          bootstrap.TextInput(
            ref = bootstrap.TextInput.ref(),
            name = "loginName",
            label = i18n("app.login-name"),
            defaultValue = user.loginName,
            disabled = true
          ),
          bootstrap.TextInput(
            ref = passwordRef,
            name = "password",
            label = i18n("app.password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          bootstrap.TextInput(
            ref = passwordVerificationRef,
            name = "passwordVerification",
            label = i18n("app.retype-password"),
            inputType = "password",
            required = true,
            showErrorMessage = state.showErrorMessages
          ),
          <.button(
            ^.tpe := "submit",
            ^.className := "btn btn-default",
            ^.onClick ==> onSubmit,
            i18n("app.ok"))
        )
      )
    }

    private def onSubmit(e: ReactEventFromInput): Callback = LogExceptionsCallback {
      val props = $.props.runNow()
      e.preventDefault()

      $.modState(state =>
        logExceptions {
          var newState = State(showErrorMessages = true)

          val maybeNewPassword = for {
            password <- passwordRef().value
            passwordVerification <- passwordVerificationRef().value
            validPassword <- {
              if (password != passwordVerification) {
                newState = newState.copy(globalErrors = Seq(i18n("app.error.passwords-should-match")))
                None
              } else {
                Some(password)
              }
            }
          } yield validPassword

          maybeNewPassword match {
            case Some(newPassword) =>
              dispatcher.dispatch(
                Action.UpsertUser(UserPrototype.create(id = user.id, plainTextPassword = newPassword)))

              // Clear form
              passwordRef().setValue("")
              passwordVerificationRef().setValue("")
              newState = State()

            case None =>
          }
          newState
      }).runNow()
    }
  }
}
