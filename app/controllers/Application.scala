package controllers

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import models.Role.Administrator
import models._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.data.Forms._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

object Application extends Controller with LoginLogout with AuthConfigImpl with AuthElement {

  private val SECRET = "6Le0JQUTAAAAADVcJyf76K3qvhUdF4ke6xwA10QS"

  /**
   * Takes page parameter to drop post list...
   * @param page
   * @return
   */
  def index(page: Int) = Action { implicit rs =>
      Ok(views.html.index(
        Posts.list(page = page,pageSize=5)
      ))
  }

  def posts(page: Int) = StackAction(AuthorityKey->Administrator) { implicit rs =>
    val user = loggedIn
      Ok(views.html.postList(
        Posts.list(page = page,pageSize=10),user
      ))
  }

  def about = Action {
    Ok(views.html.about.render())
  }

  def contact = Action { implicit request =>
    Ok(views.html.contact(Message.messageForm))
  }

  def blog(id:Long) = Action { implicit request =>
    Ok(views.html.post(Posts.postNavigator(id)))
  }

  def edit(id:Long) = StackAction(AuthorityKey->Administrator) { implicit request =>
    val post = Posts.findById(id)
    val user = loggedIn
    Ok(views.html.edit(Posts.editForm.fill(post),user))
  }

  def editPost = StackAction(AuthorityKey->Administrator) { implicit request =>
    val user = loggedIn
    Posts.editForm.bindFromRequest.fold(
       formWithErrors => BadRequest(views.html.edit(formWithErrors,user)),
       editedPost => {
         Posts.update(editedPost.id,editedPost)
         Redirect(routes.Application.edit(editedPost.id)).flashing("success"->"Gönderi içeriği başarıyla güncellendi!")
       }
    )
  }

  def sendMail = Action.async { implicit request =>
      Message.messageForm.bindFromRequest.fold(
        formwitherrors => Future.successful(BadRequest(views.html.contact(formwitherrors))),
        messageContent => {
          val g_recaptcha_response = request.body.asFormUrlEncoded.get("g-recaptcha-response")
          WS.url("https://www.google.com/recaptcha/api/siteverify").post(Map("secret"->Seq(SECRET),"response"->g_recaptcha_response)) map {
            response => {
              val success = (response.json \ "success").as[Boolean]
              if(success){
                println(messageContent)
                //TODO sent mail to the blog owner!!
                Redirect(routes.Application.contact()).flashing("success"->"Mesajınız başarıyla yollandı!")
              }
              else
                Redirect(routes.Application.contact()).flashing("error"->"Robot olmadığını kanıtlaman gerek!")
              }
            }
          }
      )
  }

  /** Your application's login form.  Alter it to fit your application */
  val loginForm = Form {
    mapping("email" -> email, "password" -> nonEmptyText)(Users.authenticate)(_.map(u => (u.email, "")))
      .verifying("Kullanıcı adı veya şifre hatalı!", result => result.isDefined)
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
    val user = loggedIn
    Ok(views.html.admin(user))
  }

}