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
   * How to index a github user.
   *
   * @param owner
   * @param token
   * @return
   */
  def indexUser(owner :String, token :String) = Action { implicit request =>
    IndexGithub.indexUser(owner, 0, 2, token)
    Ok(Json.toJson("OK"))
  }

  /**
   * How to index a github repository.
   *
   * @param owner
   * @param name
   * @param token
   * @return
   */
  def indexRepository(owner :String, name :String, token :String) = Action { implicit request =>
    IndexGithub.indexRepo(owner, name, 0, 2, token)
    Ok(Json.toJson("OK"))
  }

  /**
   * Getting user recommendation for a github user identified by its login.
   *
   * @param login
   * @return
   */
  def userRecommendation(login :String) = Action { implicit request =>
    Ok(Json.toJson(IndexGithub.getUserReco(login)))
  }

  /**
   * Getting repository recommendation for a github repository.
   *
   * @param name
   * @return
   */
  def repoRecommendation(owner :String, name :String) = Action { implicit request =>
    Ok(Json.toJson(IndexGithub.getRepositoryReco(request.getQueryString("login"), owner + "/" + name)))
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
