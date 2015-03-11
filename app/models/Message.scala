package models

import play.api.data.Form
import play.api.data.Forms._

/**
 * Created by Alper on 11.03.2015.
 */
case class MessageForm(name:String,email:String,phone:String,message:String)

object Message {
  val messageForm: Form[MessageForm] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Email" -> email,
      "Phone" -> nonEmptyText,
      "Message" -> nonEmptyText
    )(MessageForm.apply)(MessageForm.unapply)
  )
}
