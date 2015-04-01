package controllers

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import models.Role.Administrator
import models._
import play.api.data.Form
import play.api.mvc._
import play.api.data.Forms._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller with LoginLogout with AuthConfigImpl with AuthElement {

  def index(page: Int) = Action { implicit rs =>
      Ok(views.html.index(
        Posts.list(page = page)
      ))
  }

  def about = Action {
    Ok(views.html.about.render())
  }

  def contact = Action { implicit request =>
    Ok(views.html.contact(Message.messageForm))
  }

  def blog(id:Long) = Action { implicit request =>
    Ok(views.html.post(Posts.findById(id)))
  }

  def sendMail = Action { implicit request =>
      Message.messageForm.bindFromRequest.fold(
        formwitherrors => BadRequest(views.html.contact(formwitherrors)),
        messageContent => {
          println(messageContent.email)
          //TODO sent mail to the blog owner!!
          Redirect(routes.Application.contact()).flashing("success"->"Mesajınız başarıyla yollandı!")
        }
      )
  }

  /** Your application's login form.  Alter it to fit your application */
  val loginForm = Form {
    mapping("email" -> email, "password" -> nonEmptyText)(Users.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  /** Alter the login page action to suit your application. */
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  /**
   * Return the `gotoLogoutSucceeded` method's result in the logout action.
   *
   * Since the `gotoLogoutSucceeded` returns `Future[Result]`,
   * you can add a procedure like the following.
   *
   *   gotoLogoutSucceeded.map(_.flashing(
   *     "success" -> "You've been logged out"
   *   ))
   */
  def logout = Action.async { implicit request =>
    // do something...
    gotoLogoutSucceeded
  }

  /**
   * Return the `gotoLoginSucceeded` method's result in the login action.
   *
   * Since the `gotoLoginSucceeded` returns `Future[Result]`,
   * you can add a procedure like the `gotoLogoutSucceeded`.
   */
  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      user => gotoLoginSucceeded(user.get.id)
    )
  }

  def adminPage = StackAction(AuthorityKey->Administrator) { implicit request =>
    Ok("Admin page will be here")
  }

}