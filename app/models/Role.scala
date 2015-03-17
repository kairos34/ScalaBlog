package models

/**
 * Created by Alper on 17.03.2015.
 */
sealed trait Role

object Role {

  case object Administrator extends Role
  case object NormalUser extends Role

  def valueOf(value: String): Role = value match {
    case "Administrator" => Administrator
    case "NormalUser"    => NormalUser
    case _ => throw new IllegalArgumentException()
  }

}
