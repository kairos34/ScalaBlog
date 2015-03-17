package models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

import scala.language.postfixOps

/**
 * Created by Alper on 17.03.2015.
 */

case class User(id:Long,email:String,password:String,firstname:String,surname:String,role:Role)

object User {

  /**
   * Parse a User from a ResultSet
   */
  val simpleParser = {
      get[Long]("user.id") ~
      get[String]("user.email") ~
      get[String]("user.password") ~
      get[String]("user.firstname") ~
      get[String]("user.lastname") ~
      get[String]("user.role")   map {
      case id~email~password~firstname~lastname~role => User( id, email ,password ,firstname ,lastname, Role.valueOf(role))
    }
  }

  def findById(_id: Long): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from configuration.user where _id = {_id}").on(
        '_id -> _id
      ).as(User.simpleParser.singleOpt)
    }
  }

  def authenticate(email: String, password: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         select * from configuration.user where
         email = {email} and password = {password}
        """
      ).on(
          'email -> email,
          'password -> password
        ).as(User.simpleParser.singleOpt)
    }
  }
}
