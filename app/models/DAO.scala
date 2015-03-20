package models

import scala.slick.lifted.TableQuery

/**
 * Created by Alper on 20.03.2015.
 */
trait DAO {
  val posts = TableQuery[Posts]
  val users = TableQuery[Users]
}
