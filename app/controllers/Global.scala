package controllers

import play.api.GlobalSettings
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

/**
 * @author Alper 
      Created on 25.02.2016.
 */
object Global extends GlobalSettings{
  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(views.html.notFound()))
  }

}
