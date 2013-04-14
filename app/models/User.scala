package models

import play.api.db._
import play.api.Play.current
import play.api.{Logger, Application}
import anorm._
import anorm.SqlParser._
import scala.Option
import play.api.libs.json.{Json, JsNull, JsString, JsObject}
import securesocial.core
import anorm.~
import core.{OAuth2Info, Identity, SocialUser, UserId}
import anorm.Id
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some
import play.api.Play.current
import util.parsing.json.JSONObject

/**
 * User class.
 *
 * @author : bsimard
 *
 */
case class User (id: Pk[Long], firstname: String, lastname: String, fullname: String, email: Option[String], avatarUrl: Option[String])

object User {

  /**
   * Parser for ResultSet
   */
  val simple = {
    get[Pk[Long]]("user.id") ~
    get[String]("user.firstname") ~
    get[String]("user.lastname") ~
    get[String]("user.fullname") ~
    get[Option[String]]("user.email") ~
    get[Option[String]]("user.avatarUrl") map {
      case id~firstname~lastname~fullname~email~avatarUrl => User(id, firstname, lastname, fullname, email, avatarUrl)
    }
  }

  /**
   * Create a user if it doesn't exist.
   *
   * @param identity
   * @return
   */
  def createFromIdentity(identity: Identity): User = {
    User.findFromIdentityId(identity.id) match {
      case Some(user) => {user}
      case None => {
        DB.withTransaction { implicit connection =>
          // Get the project id
          val id: Long =  SQL("select next value for user_seq").as(scalar[Long].single)
          SQL(
            """
              INSERT INTO
                user
              VALUES
                (
                  {id},
                  {firstname},
                  {lastname},
                  {fullname},
                  {email},
                  {avatarUrl}
                )
            """
          ).on(
            'id -> id,
            'firstname -> identity.firstName,
            'lastname -> identity.lastName,
            'fullname -> identity.fullName,
            'email -> identity.email,
            'avatarUrl -> identity.avatarUrl
          ).executeUpdate()

          val json:Option[JsObject] = identity.oAuth2Info match {
            case Some(oauth) => {
              Some(
                Json.obj(
                  "accesToken" -> JsString(oauth.accessToken)
                )
              )
            }
            case None => None
          }

          SQL(
            """
              INSERT INTO
                userAccount
              VALUES
                (
                  {id},
                  {provider},
                  {json},
                  {user_id}
                )
            """
          ).on(
            'id -> identity.id.id,
            'provider -> identity.id.providerId,
            'json -> json.getOrElse("").toString,
            'user_id -> id
          ).executeUpdate()

          //return
          User(Id(id), identity.firstName, identity.lastName, identity.fullName, identity.email, identity.avatarUrl)
        }
      }
    }
  }

  /**
   * Find a user from Secure Social id (UserId).
   *
   * @param userId
   * @return models.User
   */
  def findFromIdentityId(userId:UserId): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          SELECT
            user.*
          FROM
            user INNER JOIN userAccount
              ON user.id = userAccount.user_id
          WHERE
            userAccount.id={id} AND
            userAccount.provider={provider}
        """
      ).on(
        'id -> userId.id,
        'provider -> userId.providerId
      ).as(User.simple.singleOpt)
    }
  }

  def toIdentity(user: User): Identity = {
    val ident: Identity = UserAccount.getByProvider(user, "github") match {
      case Some(account) => {
        account.json match {
          case Some(json) => {
            Logger.debug("JSON is " + json.as[JsObject].\("accesToken").as[String])
            val token: String = json.as[JsObject].\("accesToken").as[String]
            new SocialUser( UserId(account.id, account.provider), user.firstname, user.lastname, user.fullname, user.email, user.avatarUrl, core.AuthenticationMethod.OAuth2, None, Some(OAuth2Info(token)), None )
          }
          case None => new SocialUser( UserId("", ""), user.firstname, user.lastname, user.fullname, user.email, user.avatarUrl, core.AuthenticationMethod.OAuth2, None, None, None )
        }
      }
      case None => new SocialUser( UserId("", ""), user.firstname, user.lastname, user.fullname, user.email, user.avatarUrl, core.AuthenticationMethod.OAuth2, None, None, None )
    }
   ident
  }
}
