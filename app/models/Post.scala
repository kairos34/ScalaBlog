package models

import java.text.{SimpleDateFormat, DateFormat}
import java.util.{Locale, Calendar}
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
case class PostNavigator(post:Post,hasNext:Boolean,hasPrev:Boolean,nextTitle:String,prevTitle:String,nextId:Long,prevId:Long)

class Posts(tag: Tag) extends Table[Post](tag,"post"){
  def id = column[Long]("id",O.PrimaryKey)
  def userId = column[Long]("user_id")
  def date = column[Long]("date")
  def title = column[String]("title")
  def subTitle = column[String]("sub_title")
  def content = column[String]("content")

  def * = (id,userId,date,title,subTitle,content) <> ((Post.apply _).tupled, Post.unapply _)
}

object Posts extends DAO{

  val postList =
    (for {
      (posts, users) <- posts leftJoin users on (_.userId === _.id)
    } yield (posts))
      .sortBy(_.date.desc)

  val postForm: Form[Post] = Form(
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
    val df:DateFormat = new SimpleDateFormat("dd MMMMM yyyy",new Locale("tr"))
    val cal:Calendar = Calendar.getInstance()
      cal.setTimeInMillis(time)
      df.format(cal.getTime)
  }

  def list(page: Int = 0, pageSize: Int = 1): Page[Post] = {
  DB.withTransaction{ implicit s =>
    val offset = pageSize * page
    val query = postList
        .drop(offset)
        .take(pageSize)

    val totalRows = count
    val result = query.list

    Page(result, page, offset, totalRows)
  }
  }

  def postNavigator(id:Long) = {
    DB.withTransaction{ implicit s =>
      val list = postList.list
      val index = list.indexWhere(_.id==id)
      var nextTitle = ""
      var nextId = 0l
      var prevTitle = ""
      var prevId = 0l
      val hasNext = index+1<list.length
      val hasPrev = index>0
      if(hasPrev){
        prevTitle = list(index-1).title
        prevId = list(index-1).id
      }
      if(hasNext){
        nextTitle = list(index+1).title
        nextId = list(index+1).id
      }
      new PostNavigator(list(index),hasNext,hasPrev,nextTitle,prevTitle,nextId,prevId)
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
    val postToUpdate: Post = post.copy(id)
    DB.withTransaction{ implicit session =>
      posts.filter(_.id === id).update(postToUpdate)
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
