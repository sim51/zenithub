package controllers

import play.api.cache.Cache
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{Lang, Messages}
import play.api.Logger
import play.api.libs.json.{JsString, JsObject, JsValue, Json}
import play.api.libs.ws.{Response, WS}
import play.api.mvc._
import play.api.Play
import play.api.Play.current

import com.typesafe.plugin._
import scala.Some
import io.Source
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.concurrent.Execution.Implicits._
import securesocial.core.SecureSocial
import concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Application's controllers.
 *
 * @author bsimard
 */
object Application extends Controller with securesocial.core.SecureSocial {

  /**
   * Default action to get angularJS application.
   * @return
   */
  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(user) => {
        user.oAuth2Info match {
          case Some(oAuthInfo2) => {
            Logger.debug("Having user with token !: " + oAuthInfo2.accessToken)
            val cookie:Cookie =  Cookie("token", "\"" + oAuthInfo2.accessToken + "\"", None , "/", None, false, false)
            Ok(views.html.index(request.user)).withCookies(cookie)
          }
          case None => {
            Logger.debug("Having user without token !")
            Ok(views.html.index(request.user))
          }
        }
      }
      case None => {
        Ok(views.html.index(request.user))
      }
    }
  }
  
  /**
   * JSON action to retrive all messages.
   * @return
   */
  def messages = Action { implicit request =>
    val i18n:Map[String, Map[String, String]] = Messages.messages
    Logger.debug("Language is " + lang.code)
    if (i18n.get(lang.code).isEmpty){
      val defaultLang :Lang = new Lang("fr")
      Ok(Json.toJson(i18n.get(defaultLang.code)))
    }
    else{
      Ok(Json.toJson(i18n.get(lang.code)))
    }
  }
}
