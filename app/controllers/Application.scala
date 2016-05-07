package controllers

import controllers.Application.Forms.{ChangePasswordData, AddUserData}
import models.Users
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._


object Application extends Controller with Secured {

  // ********** actions ********** //
  def index() = ActionWithUser { implicit user =>
    implicit request =>
      Redirect(controllers.accounting.routes.Views.cashFlowOfAll)
  }

  def profile() = ActionWithUser { implicit user =>
    implicit request =>
      val initialData = ChangePasswordData(user.loginName)
      Ok(views.html.profile(Forms.changePasswordForm.fill(initialData)))
  }

  def changePassword = ActionWithUser { implicit user =>
    implicit request =>
      Forms.changePasswordForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.profile(formWithErrors)),
        formData => formData match {
          case ChangePasswordData(loginName, _, password, _) =>
            require(loginName == user.loginName)
            Users.all.save(user.withPasswordHashFromUnhashed(password))
            val message = "Successfully updated password"
            Redirect(routes.Application.profile).flashing("message" -> message)
        }
      )
  }

  def administration() = ActionWithAdminUser { implicit user =>
    implicit request =>
      Ok(views.html.administration(users = Users.all.fetchAll, Forms.addUserForm))
  }

  def addUser() = ActionWithAdminUser { implicit user =>
    implicit request =>
      Forms.addUserForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.administration(users = Users.all.fetchAll, formWithErrors)),
        formData => formData match {
          case AddUserData(loginName, name, password, _) =>
            Users.all.save(Users.newWithUnhashedPw(loginName, password, name))
            val message = s"Successfully added user $name"
            Redirect(routes.Application.administration).flashing("message" -> message)
        }
      )
  }

  // ********** forms ********** //
  object Forms {

    case class ChangePasswordData(loginName: String,
                                  oldPassword: String = "",
                                  password: String = "",
                                  passwordVerification: String = "")

    val changePasswordForm = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "oldPassword" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(ChangePasswordData.apply)(ChangePasswordData.unapply) verifying("Old password is incorrect", result => result match {
        case ChangePasswordData(loginName, oldPassword, _, _) => Users.authenticate(loginName, oldPassword)
      }) verifying("Passwords should match", result => result match {
        case ChangePasswordData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )

    case class AddUserData(loginName: String,
                           name: String = "",
                           password: String = "",
                           passwordVerification: String = "")

    val addUserForm = Form(
      mapping(
        "loginName" -> nonEmptyText,
        "name" -> nonEmptyText,
        "password" -> nonEmptyText,
        "passwordVerification" -> nonEmptyText
      )(AddUserData.apply)(AddUserData.unapply) verifying("Passwords should match", result => result match {
        case AddUserData(_, _, password, passwordVerification) => password == passwordVerification
      })
    )
  }

}