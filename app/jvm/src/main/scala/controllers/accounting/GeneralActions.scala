package controllers.accounting

import com.google.inject.Inject
import controllers.helpers.accounting.GeneralEntry
import play.api.mvc._

import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import models.User
import models.accounting.UpdateLogs
import models.accounting.config.{Config, Template}
import controllers.helpers.AuthenticatedAction

class GeneralActions @Inject()(val messagesApi: MessagesApi, accountingConfig: Config) extends Controller with I18nSupport {

  // ********** actions ********** //
  def searchMostRelevant(q: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      search(q, numEntriesToShow = 200)
  }

  def searchAll(q: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      search(q)
  }

  def updateLogsLatest = AuthenticatedAction { implicit user =>
    implicit request =>
      updateLogs(numEntriesToShow = 400)
  }

  def updateLogsAll = AuthenticatedAction { implicit user =>
    implicit request =>
      updateLogs()
  }

  def templateList = AuthenticatedAction { implicit user =>
    implicit request =>
      Ok(views.html.accounting.templatelist(
        templates = accountingConfig.templatesToShowFor(Template.Placement.TemplateList, user)))
  }

  // ********** private helper controllers ********** //
  private def search(query: String, numEntriesToShow: Int = 100000)
                    (implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val allEntries = GeneralEntry.search(query)
    val entries = allEntries.take(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.search(
      query = query,
      totalNumResults = allEntries.size,
      entries = entries,
      numEntriesToShow = numEntriesToShow,
      templatesInNavbar = accountingConfig.templatesToShowFor(Template.Placement.SearchView, user)))
  }

  private def updateLogs(numEntriesToShow: Int = 100000)(implicit request: Request[AnyContent], user: User): Result = {
    // get entries
    val entries = UpdateLogs.fetchLastNEntries(numEntriesToShow + 1)

    // render
    Ok(views.html.accounting.updateLogs(
      entries = entries,
      numEntriesToShow = numEntriesToShow))
  }
}
