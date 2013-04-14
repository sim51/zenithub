package models

import play.api.db.DB
import anorm._
import anorm.SqlParser._
import anorm.~
import play.api.libs.json.{JsValue, Json, JsObject}
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 *
 * @author : bsimard
 */
case class UserAccount (id: String, provider: String, json: Option[JsValue])

object UserAccount {

  /**
   * Parser for ResultSet
   */
  val simple = {
    get[String]("userAccount.id") ~
    get[String]("userAccount.provider") ~
    get[Option[String]]("userAccount.json") map {
    case id~provider~json => UserAccount(id, provider, Some(Json.parse(json.getOrElse(""))))
    }
  }

  /**
   *
   * @param user
   * @param provider
   * @return
   */
  def getByProvider(user:User, provider:String): Option[UserAccount] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          SELECT
            userAccount.*
          FROM
            userAccount
          WHERE
            userAccount.user_id={id} AND
            userAccount.provider={provider}
        """
      ).on(
        'id -> user.id,
        'provider -> provider
      ).as(UserAccount.simple.singleOpt)
    }
  }
}