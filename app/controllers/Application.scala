package controllers

import models.Message
import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def about = Action {
    Ok(views.html.about.render())
  }

  def contact = Action { implicit request =>
    Ok(views.html.contact(Message.messageForm))
  }

  def sendMail = Action {
    implicit request =>
      Message.messageForm.bindFromRequest.fold(
        formwitherrors => BadRequest(views.html.contact(formwitherrors)),
        messageContent => {
          println(messageContent.email)
          //TODO sent mail to the blog owner!!
          Redirect(routes.Application.contact()).flashing("success"->"Mesajınız başarıyla yollandı!")
        }
      )
  }

}