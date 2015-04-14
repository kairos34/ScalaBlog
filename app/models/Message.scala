package models

import play.api.data.Form
import play.api.data.Forms._

/**
 * Created by Alper on 11.03.2015.
 */
case class MessageForm(name:String,email:String,phone:Long,message:String)

object Message {
  val messageForm: Form[MessageForm] = Form(
    mapping(
      "Name" -> nonEmptyText,
      "Email" -> email,
      "Phone" -> longNumber,
      "Message" -> nonEmptyText
    )(MessageForm.apply)(MessageForm.unapply)
  )
}
