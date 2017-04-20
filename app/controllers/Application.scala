package controllers

import java.io.ByteArrayInputStream
import java.util.{Calendar, Random}

import com.google.common.io.ByteStreams
import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import models.Role.{NormalUser, Administrator}
import models._
import play.Play
import play.api.data.Form
import play.api.libs.iteratee.Enumerator
import play.api.libs.mailer._
import play.api.libs.ws.WS
import play.api.mvc._
import play.api.data.Forms._
import utils.HashFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

object Application extends Controller with LoginLogout with AuthConfigImpl with AuthElement {

  private val SECRET = "6Le0JQUTAAAAADVcJyf76K3qvhUdF4ke6xwA10QS"

  /**
   * Takes page parameter to drop post list according to page number...
   * This one just for showing blogs' previews...
   * @param page
   * @return postlist page
   */
  def index(page: Int) = Action { implicit request =>
      Ok(views.html.index(
        Posts.list(page = page,pageSize=10)
      ))
  }

  /**
   * Takes page parameter to drop post list according to page number just like post list...
   * This function can be executed by people who have at least NormalUser authority...
   * @param page
   * @return postlist page
   */
  def posts(page: Int) = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val user = loggedIn
      Ok(views.html.postList(
        Posts.list(page = page,pageSize=10),user
      ))
  }

  /**
   * Takes page parameter to drop user list according to page number just like post list...
   * User operations can be executed by "Admninistrator" ...
   * @param page
   * @return userList page
   */
  def users(page: Int) = StackAction(AuthorityKey->Administrator) { implicit  requesy =>
    val user = loggedIn
    Ok(views.html.userList(
        Users.list(page = page,pageSize=10), user
    ))
  }

  /**
   * It just renders about.scala.html page...
   * @return about page
   */
  def about = Action {
    Ok(views.html.about.render())
  }

  /**
   * It renders contact.scala.html page with contact form...
   * @return contact page
   */
  def contact = Action { implicit request =>
    Ok(views.html.contact(Message.messageForm))
  }

  /**
   * This function renders blog page according to blog id...
   * but for one page navigation it takes previous and next blogs' id and subtitles whether they are available...
   * @param id
   * @return blog page
   */
  def blog(id:Long) = Action { implicit request =>
    Ok(views.html.post(Posts.postNavigator(id)))
  }

  /**
   * This function takes blog id in order to render edit page with blog's information...
   * All blog actions can be executed by "Normal User"...
   * @param id
   * @return blog editing page
   */
  def edit(id:Long) = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val post = Posts.findById(id)
    val user = loggedIn
    Ok(views.html.editPost(Posts.postForm.fill(post),user))
  }

  /**
   * This function handles blog edit form submission
   * If it has errors,it will render page with error form,
   * else saves post changes to database and returns success flash scope to edit page...
   * @return blog editing page
   */
  def editPost = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val user = loggedIn
    Posts.postForm.bindFromRequest.fold(
       formWithErrors => BadRequest(views.html.editPost(formWithErrors,user)),
       editedPost => {
         Posts.update(editedPost.id,editedPost)
         Redirect(routes.Application.edit(editedPost.id)).flashing("success"->"Gönderi içeriği başarıyla güncellendi!")
       }
    )
  }

  /**
   * Contact form handling function checks form,and if it has not got any error sends email to the blog owner,
   * else renders error form...
   * @return contact page
   */
  def sendMail = Action.async { implicit request =>
      Message.messageForm.bindFromRequest.fold(
        formwitherrors => Future.successful(BadRequest(views.html.contact(formwitherrors))),
        messageContent => {
          val g_recaptcha_response = request.body.asFormUrlEncoded.get("g-recaptcha-response")
          WS.url("https://www.google.com/recaptcha/api/siteverify").post(Map("secret"->Seq(SECRET),"response"->g_recaptcha_response)) map {
            response => {
              val success = (response.json \ "success").as[Boolean]
              if(success){
                val email = Email("Yeni mesajınız var...",messageContent.email,Seq("alper.ytu7@gmail.com"),Some("Gönderenin Adı: "+messageContent.name+" ,Telefon Numarası: "+messageContent.phone+
                  " ,Email Adresi: "+messageContent.email+" Mesajı: "+messageContent.message)
                )
                MailerPlugin.send(email)
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

  /**
   * If login form submission authenticates user , he/she will be redirected to the this page...
   * @return admin page
   */
  def adminPage = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val user = loggedIn
    Ok(views.html.admin(user))
  }

  /**
   * It takes post id in order to delete it.Then returns flash scope to the post list...
   * @param id
   * @return post list page
   */
  def deletePost(id:Long) = StackAction(AuthorityKey->NormalUser) { implicit request =>
    Posts.delete(id)
    Redirect(routes.Application.posts()).flashing("success"->"İstediğiniz gönderi başarılı bir şekilde silindi!")
  }

  /**
   * It takes user id in order to delete it.Then returns flash scope to the user list
   * This function can be executed by user who has "Administrator" role...
   * @param id
   * @return user list page
   */
  def deleteUser(id:Long) = StackAction(AuthorityKey->Administrator) { implicit request =>
    Users.delete(id)
    Redirect(routes.Application.users()).flashing("success"->"İstediğiniz kullanıcı başarılı bir şekilde silindi!")
  }

  /**
   * This function renders adding post form it hides post id, date and user id from the user...
   * @return
   */
  def add = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val user = loggedIn
    Ok(views.html.createPost(Posts.postForm.fill(new Post(new Random().nextLong(),user.id,Calendar.getInstance().getTimeInMillis,"","","")),user))
  }

  /**
   * Post form submission function handles requests whether they are available to add post the database or not...
   * After adding to database it redirects to post list page with success flash scope...
   * @return post list page
   */
  def addPost = StackAction(AuthorityKey->NormalUser) { implicit request =>
    val user = loggedIn
    Posts.postForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createPost(formWithErrors,user)),
      createdPost => {
        Posts.insert(createdPost)
        Redirect(routes.Application.posts()).flashing("success"->"Yeni gönderiniz başarıyla yaratıldı!")
      }
    )
  }

  /**
   * This function renders adding user page..
   * Only administrators can execute this function otherwise "No permission" will be rendered...
   * @return add user page
   */
  def addUserPage = StackAction(AuthorityKey->Administrator) { implicit  request =>
    val user = loggedIn
    Ok(views.html.createUser(Users.userForm.fill(new UserForm(new Random().nextLong(),"","","","","")),user))
  }

  /**
   * Add user form submission function checks whether form data is available to add user to the database...
   * @return user list page
   */
  def addUser = StackAction(AuthorityKey->Administrator) { implicit request =>
    val user = loggedIn
    Users.userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.createUser(formWithErrors,user)),
      createdUser => {
        Users.insert(new User(createdUser.id,createdUser.email,HashFactory.hash(createdUser.password),createdUser.fullname,Role.valueOf(createdUser.permission)))
        Redirect(routes.Application.users()).flashing("success"->"Yeni kullanıcı başarıyla yaratıldı!")
      }
    )
  }


  /**
   * This function renders editing user page...
   * Only administrators can execute this function otherwise "No permission" will be rendered....
   * @return edit user page
   */
  def editUser(id:Long) = StackAction(AuthorityKey->Administrator) { implicit request =>
    val user = Users.findById(id).get
    val loggedUser = loggedIn
    Ok(views.html.editUser(Users.userForm.fill(new UserForm(user.id,user.email,user.fullname,"",
    "",user.role.toString)),loggedUser))
  }

  /**
   * Edit user form submission function checks whether form data is available to update user information...
   * @return edit user page
   */
  def editUserRequest = StackAction(AuthorityKey->Administrator) { implicit request =>
    val user = loggedIn
    Users.userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.editUser(formWithErrors,user)),
      editedUser => {
        Users.update(editedUser.id,new User(editedUser.id,editedUser.email,HashFactory.hash(editedUser.password),editedUser.fullname,Role.valueOf(editedUser.permission)))
        Redirect(routes.Application.editUser(editedUser.id)).flashing("success"->"Kullanıcı bilgileri başarıyla güncellendi!")
      }
    )
  }

  /**
   * This function returns CV file
   * @return CV file
   */
  def showCV = Action {
    val pdfAsByteArray = ByteStreams.toByteArray(play.Play.application.resourceAsStream("public/AlperCV.pdf"))
    val ticketInputStream = new ByteArrayInputStream(pdfAsByteArray)
    val fileContent = Enumerator.fromStream(ticketInputStream)
    val headers = Map(
      CONTENT_LENGTH -> pdfAsByteArray.length.toString,
      CONTENT_DISPOSITION -> "inline"
    )
    Result(ResponseHeader(200, headers), fileContent
    ).as("application/pdf")
  }
}