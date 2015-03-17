package models

import java.text.{SimpleDateFormat, DateFormat}
import java.util.Calendar

/**
 * Created by Alper on 17.03.2015.
 */

case class Post(id:Long,user:User,date:Long,title:String,subTitle:String,content:String)

object Post {

 /** Long to date for posting date **/
  def convertDate(time:Option[Long]):String = {
    val df:DateFormat = new SimpleDateFormat("dd MMM yyyy")
    val cal:Calendar = Calendar.getInstance()
      cal.setTimeInMillis(time.get)
      return df.format(cal.getTime)
  }
}
