package models

import models.Posts._
import play.api.data.Form
import play.api.data.Forms._
import utils.HashFactory

import scala.language.postfixOps
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import play.api.db.slick.DB
import play.api.Play.current

/**
 * Created by Alper on 17.03.2015.
 */

case class User(id:Long,email:String,password:String,fullname:String,role:Role)
case class UserForm(id:Long,email:String,fullname:String,password:String,password2:String,permission:String)

class Users(tag: Tag) extends Table[User](tag,"user"){
  def id = column[Long]("id", O.PrimaryKey)
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

  val userForm:Form[UserForm] = Form(
    mapping(
      "Id" -> longNumber,
      "Email" -> email,
      "FullName" -> nonEmptyText,
      "Password" -> text(minLength = 6),
      "Confirm Password" -> nonEmptyText,
      "Permission" -> nonEmptyText
    )(UserForm.apply)(UserForm.unapply)verifying ("Kullanıcı şifreleri eşleşmiyor!", f => f.password == f.password2)
  )


  def list(page: Int = 0, pageSize: Int = 1): Page[User] = {
    DB.withTransaction{ implicit s =>
      val offset = pageSize * page
      val totalRows = count
      val result = users.list
        .drop(offset)
        .take(pageSize)

      Page(result, page, offset, totalRows)
    }
  }

  def count: Int = {
    DB.withTransaction { implicit session =>
      Query(users.length).first
    }
  }

  def findById(id: Long): Option[User] = {
    DB.withTransaction { implicit session =>
      users.filter(u => u.id === id).firstOption
    }
  }

  def authenticate(email:String,password:String):Option[User] = {
    DB.withTransaction{ implicit session =>
      users.filter(u => u.email === email && u.password === HashFactory.hash(password)).firstOption
    }
  }

  def insert(user: User) {
    DB.withTransaction{ implicit session =>
      users.insert(user)
    }
  }

  def update(id: Long, user: User) {
    val userToUpdate: User = user.copy(id)
    DB.withTransaction{ implicit session =>
      users.filter(_.id === id).update(userToUpdate)
    }
  }

  def delete(id: Long) {
    DB.withTransaction{ implicit session =>
      users.filter(_.id === id).delete
    }
  }

}
