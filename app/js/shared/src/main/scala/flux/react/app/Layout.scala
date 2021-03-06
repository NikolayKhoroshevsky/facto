package flux.react.app

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import flux.action.{Action, Dispatcher}
import flux.react.ReactVdomUtils.^^
import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.PackageBase.VdomAttr
import japgolly.scalajs.react.vdom.html_<^._
import models.access.JsEntityAccess
import models.user.User
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class Layout(implicit globalMessages: GlobalMessages,
                   pageLoadingSpinner: PageLoadingSpinner,
                   applicationDisconnectedIcon: ApplicationDisconnectedIcon,
                   pendingModificationsCounter: PendingModificationsCounter,
                   menu: Menu,
                   user: User,
                   i18n: I18n,
                   jsEntityAccess: JsEntityAccess,
                   dispatcher: Dispatcher) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      val router = props.router
      <.div(
        ^.id := "wrapper",
        // Navigation
        <.nav(
          ^.className := "navbar navbar-default navbar-static-top",
          ^.role := "navigation",
          ^.style := js.Dictionary("marginBottom" -> 0),
          <.div(
            ^.className := "navbar-header",
            <.button(
              ^.tpe := "button",
              ^.className := "navbar-toggle",
              VdomAttr("data-toggle") := "collapse",
              VdomAttr("data-target") := ".navbar-collapse",
              <.span(^.className := "sr-only", "Toggle navigation"),
              <.span(^.className := "icon-bar"),
              <.span(^.className := "icon-bar"),
              <.span(^.className := "icon-bar")
            ),
            router.anchorWithHrefTo(Page.Root)(^.className := "navbar-brand", "Family Accounting Tool"),
            " ",
            pageLoadingSpinner()
          ),
          <.ul(
            ^.className := "nav navbar-top-links navbar-right",
            applicationDisconnectedIcon(),
            pendingModificationsCounter(),
            <.li(
              ^.className := "dropdown",
              <.a(
                ^.className := "dropdown-toggle",
                VdomAttr("data-toggle") := "dropdown",
                ^.href := "#",
                <.i(^.className := "fa fa-user fa-fw"),
                " ",
                <.i(^.className := "fa fa-caret-down")
              ),
              <.ul(
                ^.className := "dropdown-menu dropdown-user",
                <.li(
                  router
                    .anchorWithHrefTo(Page.UserProfile)(
                      <.i(^.className := Page.UserProfile.iconClass),
                      " ",
                      Page.UserProfile.title
                    )),
                ^^.ifThen(user.isAdmin) {
                  <.li(
                    router
                      .anchorWithHrefTo(Page.UserAdministration)(
                        <.i(^.className := Page.UserAdministration.iconClass),
                        " ",
                        Page.UserAdministration.title
                      ))
                },
                <.li(^.className := "divider"),
                <.li(
                  <.a(
                    ^.href := "/logout/",
                    ^.onClick ==> doLogout,
                    <.i(^.className := "fa fa-sign-out fa-fw"),
                    " ",
                    i18n("app.logout")))
              )
            )
          ),
          <.div(
            ^.className := "navbar-default sidebar",
            ^.role := "navigation",
            <.div(
              ^^.classes(Seq("sidebar-nav", "navbar-collapse") ++ ifThenSeq(navbarCollapsed, "collapse")),
              menu(router))
          )
        ),
        // Page Content
        <.div(
          ^.id := "page-wrapper",
          ^.style := js.Dictionary("minHeight" -> s"${pageWrapperHeight}px"),
          <.div(
            ^.className := "container-fluid",
            <.div(
              ^.className := "row",
              <.div(
                ^.className := "col-lg-12",
                globalMessages(),
                children,
                <.hr(),
                <.span(^.dangerouslySetInnerHtml := "&copy;"),
                " 2018 Jens Nyman"))
          )
        )
      )
    }
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private helper methods ****************//
  private def navbarCollapsed: Boolean = {
    // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val width = if (dom.window.innerWidth > 0) dom.window.innerWidth else dom.window.screen.width
    width < 768
  }
  private def pageWrapperHeight: Int = {
    // Based on Start Bootstrap code in assets/startbootstrap-sb-admin-2/dist/js/sb-admin-2.js
    val topOffset = if (navbarCollapsed) 100 else 50

    val windowHeight = if (dom.window.innerHeight > 0) dom.window.innerHeight else dom.window.screen.height
    windowHeight.toInt - 1 - topOffset
  }

  private def doLogout(e: ReactMouseEvent): Callback = LogExceptionsCallback {
    e.preventDefault()
    dispatcher.dispatch(Action.SetPageLoadingState(isLoading = true))
    jsEntityAccess.clearLocalDatabase() map { _ =>
      dom.window.location.href = "/logout/"
    }
  }

  private def ifThenSeq[V](condition: Boolean, value: V): Seq[V] = if (condition) Seq(value) else Seq()

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
