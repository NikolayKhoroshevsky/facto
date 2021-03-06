package flux.react.router

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.action.{Action, Dispatcher}
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

import scala.reflect.ClassTag

private[router] final class RouterFactory(implicit reactAppModule: flux.react.app.Module,
                                          dispatcher: Dispatcher,
                                          i18n: I18n) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until(RouterFactory.pathPrefix), routerConfig)
  }

  private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        import dsl._
        val codeString: RouteB[String] = string("[a-zA-Z0-9_-]+")
        val returnToPath: RouteB[Option[String]] = ("?returnto=" ~ string(".+")).option
        val query: RouteB[String] = "?q=" ~ string(".+")

        def staticRuleFromPage(page: Page, renderer: RouterContext => VdomElement): dsl.Rule = {
          val path = RouterFactory.pathPrefix + page.getClass.getSimpleName.toLowerCase
          staticRoute(path, page) ~> renderR(ctl => logExceptions(renderer(RouterContext(page, ctl))))
        }
        def dynamicRuleFromPage[P <: Page](dynamicPart: String => RouteB[P])(
            renderer: (P, RouterContext) => VdomElement)(implicit pageClass: ClassTag[P]): dsl.Rule = {
          val staticPathPart = RouterFactory.pathPrefix + pageClass.runtimeClass.getSimpleName.toLowerCase
          val path = dynamicPart(staticPathPart)
          dynamicRouteCT(path) ~> dynRenderR {
            case (page, ctl) => logExceptions(renderer(page, RouterContext(page, ctl)))
          }
        }

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute(RouterFactory.pathPrefix, Page.Root)
            ~> redirectToPage(Page.CashFlow)(Redirect.Replace)

          | staticRuleFromPage(Page.UserProfile, reactAppModule.userProfile.apply)

          | staticRuleFromPage(Page.UserAdministration, reactAppModule.userAdministration.apply)

          | staticRuleFromPage(Page.Everything, reactAppModule.everything.apply)

          | staticRuleFromPage(Page.CashFlow, reactAppModule.cashFlow.apply)

          | staticRuleFromPage(Page.Liquidation, reactAppModule.liquidation.apply)

          | staticRuleFromPage(Page.Endowments, reactAppModule.endowments.apply)

          | staticRuleFromPage(Page.Summary, reactAppModule.summary.apply)

          | dynamicRuleFromPage(_ ~ query.caseClass[Page.Search]) { (page, ctl) =>
            reactAppModule.searchResults(page.query, ctl)
          }
          | staticRuleFromPage(Page.TemplateList, reactAppModule.templateList.apply)

          | dynamicRuleFromPage(_ ~ returnToPath.caseClass[Page.NewTransactionGroup]) { (page, ctl) =>
            reactAppModule.transactionGroupForm.forCreate(page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(_ / (long ~ returnToPath).caseClass[Page.EditTransactionGroup]) {
            (page, ctl) =>
              reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(
            _ / (codeString ~ returnToPath).caseClass[Page.NewTransactionGroupFromReservoir]) { (page, ctl) =>
            reactAppModule.transactionGroupForm.forReservoir(page.reservoirCode, page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(_ / (codeString ~ returnToPath).caseClass[Page.NewFromTemplate]) {
            (page, ctl) =>
              reactAppModule.transactionGroupForm.forTemplate(page.templateCode, page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(
            _ / ((codeString / codeString) ~ returnToPath).caseClass[Page.NewForRepayment]) { (page, ctl) =>
            reactAppModule.transactionGroupForm.forRepayment(
              page.accountCode1,
              page.accountCode2,
              page.returnToPath,
              ctl)
          }

          | dynamicRuleFromPage(_ / (codeString ~ returnToPath).caseClass[Page.NewBalanceCheck]) {
            (page, ctl) =>
              reactAppModule.balanceCheckForm.forCreate(page.reservoirCode, page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(_ / (long ~ returnToPath).caseClass[Page.EditBalanceCheck]) { (page, ctl) =>
            reactAppModule.balanceCheckForm.forEdit(page.balanceCheckId, page.returnToPath, ctl)
          }

        // Fallback
        ).notFound(redirectToPage(Page.CashFlow)(Redirect.Replace))
          .onPostRender((prev, cur) =>
            LogExceptionsCallback(dispatcher.dispatch(Action.SetPageLoadingState(isLoading = false))))
          .setTitle(page => s"${page.title} | Facto")
      }
      .renderWith(layout)
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(
      implicit reactAppModule: flux.react.app.Module) = {
    reactAppModule.layout(RouterContext(resolution.page, routerCtl))(
      <.div(^.key := resolution.page.toString, resolution.render()))
  }
}
private[router] object RouterFactory {
  val pathPrefix = "/app/"
}
