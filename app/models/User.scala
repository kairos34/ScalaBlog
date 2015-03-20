package models

import scala.language.postfixOps
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import play.api.db.slick.DB
import play.api.Play.current

/**
 * Created by Alper on 17.03.2015.
 */

case class User(id:Long,email:String,password:String,fullname:String,role:Role)

class Users(tag: Tag) extends Table[User](tag,"user"){
  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("password")
  def fullname = column[String]("full_name")
  def role = column[String]("role")

  def * = (id, email, password, fullname ,role) <>
    ((t : (Long,
      String, String, String, String)) => User(t._1, t._2, t._3, t._4, Role.valueOf(t._5)),
      (u: User) => Some((u.id,u.email,u.password,u.fullname,u.role.toString)))
}
object Users extends DAO{

  def findById(id: Long): Option[User] = {
    DB.withTransaction { implicit session =>
      users.filter(u => u.id === id).firstOption
    }
  }

  def authenticate(email:String,password:String):Option[User] = {
    DB.withTransaction{ implicit session =>
      users.filter(u => u.email === email && u.password === password).firstOption
    }
  }

}
