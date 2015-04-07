package models

import java.text.{SimpleDateFormat, DateFormat}
import java.util.{Calendar}
import play.api.data.Form
import play.api.data.Forms._
import play.api.db.slick.DB
import scala.language.postfixOps
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag
import play.api.Play.current

/**
 * Created by Alper on 17.03.2015.
 */

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

case class Post(id:Long,userId:Long,date:Long,title:String,subTitle:String,content:String)

class Posts(tag: Tag) extends Table[Post](tag,"post"){
  def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
  def userId = column[Long]("user_id")
  def date = column[Long]("date")
  def title = column[String]("title")
  def subTitle = column[String]("sub_title")
  def content = column[String]("content")

  def * = (id,userId,date,title,subTitle,content) <> ((Post.apply _).tupled, Post.unapply _)
}

object Posts extends DAO{

  val editForm: Form[Post] = Form(
    mapping(
      "Id" -> longNumber,
      "UserId" -> longNumber,
      "Date" -> longNumber,
      "Title" -> nonEmptyText,
      "SubTitle" -> nonEmptyText,
      "Content" -> nonEmptyText
    )(Post.apply)(Post.unapply)
  )

 /** Long to date for posting date **/
  def convertDate(time:Long):String = {
    val df:DateFormat = new SimpleDateFormat("dd MMMMM yyyy")
    val cal:Calendar = Calendar.getInstance()
      cal.setTimeInMillis(time)
      df.format(cal.getTime)
  }

  def list(page: Int = 0, pageSize: Int = 2): Page[Post] = {
  DB.withTransaction{ implicit s =>
    val offset = pageSize * page
    val query =
      (for {
        (posts, users) <- posts leftJoin users on (_.userId === _.id)
      } yield (posts))
        .sortBy(_.date.desc)
        .drop(offset)
        .take(pageSize)

    val totalRows = count
    val result = query.list

    Page(result, page, offset, totalRows)
  }
  }

  def count: Int = {
    DB.withTransaction { implicit session =>
      Query(posts.length).first
    }
  }

  def insert(post: Post) {
    DB.withTransaction{ implicit session =>
      posts.insert(post)
    }
  }

  def update(id: Long, post: Post) {
    val computerToUpdate: Post = post.copy(id)
    DB.withTransaction{ implicit session =>
      posts.filter(_.id === id).update(computerToUpdate)
    }
  }

  def delete(id: Long) {
    DB.withTransaction{ implicit session =>
      posts.filter(_.id === id).delete
    }
  }

  def findById(id: Long) = {
    DB.withTransaction { implicit session =>
      posts.filter(_.id === id).first
    }
  }
}
