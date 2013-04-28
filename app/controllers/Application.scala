package controllers

import play.api.i18n.{Lang, Messages}
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{Response, WS}
import play.api.mvc._
import play.api.Play.current

import scala.Some
import play.api.libs.concurrent.Execution.Implicits._
import concurrent.Future
import services.{IndexGithub, Neo4j}
import org.neo4j.graphdb._

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
    Logger.debug("Neo4j " + Neo4j.graphDb.getNodeById(0).getId)
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

  def indexUser(owner :String, token :String) = Action { implicit request =>
    IndexGithub.indexUser(owner, token , 0, 2)
    Ok(Json.toJson("OK"))
  }

}
