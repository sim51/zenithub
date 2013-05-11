package services

import org.neo4j.graphdb._
import org.neo4j.graphdb.index.Index
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.WS
import scala.collection.JavaConverters._
import ExecutionContext.Implicits.global
import play.api.Logger
import play.api.libs.ws.Response
import play.api.libs.json.{Json, JsValue, JsObject}
import java.util.{Random, Date}
import java.text.SimpleDateFormat
import org.neo4j.cypher.{ExecutionResult, ExecutionEngine}

/**
 * Class to save and index gtihub with some neo4j helper.
 *
 * @author : bsimard
 */
object IndexGithub {

  /**
   * Node name for user (also for root relation & index name)
   */
  val NODE_USER: String = "USER"

  /**
   * Node name for repository (also for root relation & index name)
   */
  val NODE_REPOSITORY: String = "REPOSITORY"

  /**
   * Relation name for followers.
   */
  val REL_FOLLOW: String = "FOLLOW"

  /**
   * Relation name for watchers.
   */
  val REL_WATCHER: String = "WATCH"

  /**
   * Relation name for starers.
   */
  val REL_STARE: String = "STARE"

  /**
   * Relation name for forks.
   */
  val REL_FORK: String = "IS_A_FORK_OF"

  /**
   * Relation name for contributors.
   */
  val REL_CONTRIBUTOR: String = "HAS_CONTRIBUTED"

  /**
   * Relation name for creating a repo.
   */
  val REL_CREATE: String = "HAS_CREATED"

  /**
   * Github html url
   */
  val GITHUB_HTML_URL: String = "https://github.com"

  /**
   * Github api url
   */
  val GITHUB_API_URL: String = "https://api.github.com"

  val PER_PAGE: Int = 10000;

  /**
   * Index github user into neo4j database.
   *
   * @param login
   */
  def indexUser(login: String, avatar :Option[String], depth: Int, maxDepth: Int, token: String) {
    Logger.debug("Indexing github user " + login)
    // Create nodes
    val user: Node = getOrSaveUser(login, avatar, token)

    // we index user, only
    if (depth < maxDepth) {

      // we only index user, if last one has been made more then 24h.
      if(shouldIndex(user)) {

        Logger.debug("Going deeper for " + login)

        // get all following : create relation and index user
        var url :String = GITHUB_API_URL + "/users/" + login + "/following?per_page=" + PER_PAGE + "&" + githubAuthParam(token)
        Logger.debug("Get all following for " + login + " => " + url)
        indexUserFromAPIUserReturnUser(url, "following", login, depth, maxDepth, token)

        // get all stares repos : create relation and index user
        url = GITHUB_API_URL + "/users/" + login + "/starred?per_page=" + PER_PAGE + "&" + githubAuthParam(token)
        Logger.debug("Get all stare repos for " + login + " => " + url)
        indexRepoFromAPIUserReturnRepositories(url, login, REL_STARE, maxDepth, maxDepth, token)

        setIndexed(user)

      }
    }
  }

  /**
   * Index github current user into neo4j database.
   *
   */
  def indexCurrentUser(depth: Int, maxDepth: Int, token: String) {
    val userUrl = GITHUB_API_URL + "/user?" + githubAuthParam(token)
    val futureReposResp: Future[Response] = WS.url(userUrl).get()
    futureReposResp.map {
      response =>
        Logger.debug("Github response code is : " + response.status + " for " + userUrl)
        if (response.status == 200) {
          val json: JsValue = response.json
          val login: String = json.\("login").toString().replace("\"", "")
          val avatar: String = json.\("avatar_url").toString().replace("\"", "")
          indexUser(login, Some(avatar), depth, maxDepth, token)
        }
    }
  }

  /**
   * Index users (ie watchers, stares, contributors) from a repository API call that return user.
   *
   * @param url
   * @param relation
   * @param repo
   */
  def indexUserFromAPIRepoReturnedUser(url: String, relation: String, repo: Node, depth: Int, maxDepth: Int, token: String) {
    val futureResp: Future[Response] = getGithubResponse(url, repo)
    futureResp.map {
      response =>
        Logger.debug("Github response code is : " + response.status + " for " + url)
        if (response.status == 200) {
          val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
          Logger.debug("Url " + url + " has " + json.size + " relation of type " + relation)
          for (jsObject <- json) {
            val login: String = jsObject.\("login").toString().replace("\"", "")
            val avatar: Option[String] = Some(jsObject.\("avatar_url").toString().replace("\"", ""))
            val user: Node = getOrSaveUser(login, avatar, token)
            createRelationship(relation, user, repo)
            Logger.debug("Indexing user " + login + " due to " + relation + " with " + repo.getProperty("name"))
            indexUser(login, None, depth + 1, maxDepth, token)
          }
          response.header("Link") match {
            case Some(link) => {
              var links: Array[String] = link.split(",")
              links = links.filter(link =>
                if (link.contains("rel=\"next\"")) {
                  true
                } else {
                  false
                }
              )
              if (links.size > 0) {
                val nextUrl: String = links(0).split(";")(0).replace("<", "").replace(">", "")
                Logger.debug("There is some user stuff to do, SO calling url " + nextUrl)
                indexUserFromAPIRepoReturnedUser(nextUrl, relation, repo, depth, maxDepth, token)
              }
            }
          }
        }
    }
  }

  /**
   * Index github repo into neo4j database.
   *
   * @param login
   * @param repository
   */
  def indexRepo(login: String, repository: String, depth: Int, maxDepth: Int, token: String) {
    Logger.debug("Indexing github repo " + repository + " by " + login)
    // Create nodes
    val repo: Node = getOrSaveRepo(login, repository, None, token)

    // if we have to go deeper, let's index forks, watchers, stares & contributors
    if (depth < maxDepth) {

      // we only index, if last one has been made more then 24h.
      if(shouldIndex(repo)) {

        Logger.debug("Going deeper for " + login + "/" + repository)

        // get all contributors
        val url :String = GITHUB_API_URL + "/repos/" + login + "/" + repository + "/contributors?per_page=" + PER_PAGE + "&" + githubAuthParam(token)
        Logger.debug("Get all contributors for " + login + "/" + repository + " => " + url)
        indexUserFromAPIRepoReturnedUser(url: String, REL_CONTRIBUTOR, repo, depth, maxDepth, token)

        setIndexed(repo)

      }
    }
  }

  /**
   * Index users (ie watchers, stares, contributors) from a repository API call that return repositories.
   *
   * @param url
   * @param relation
   * @param repo
   */
  def indexUserFromAPIRepoReturnedRepositories(url: String, relation: String, repo: Node, depth: Int, maxDepth: Int, token: String) {
    val futureResp: Future[Response] = getGithubResponse(url, repo)
    futureResp.map {
      response =>
        Logger.debug("Github response code is : " + response.status + " for " + url)
        if (response.status == 200) {
          val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
          Logger.debug("Url " + url + " has " + json.size + " relation of type " + relation)
          for (jsObject <- json) {
            val watcher: String = jsObject.\("owner").\("login").toString().replace("\"", "")
            val avatar: Option[String] = Some(jsObject.\("owner").\("avatar_url").toString().replace("\"", ""))
            val watcherNode: Node = getOrSaveUser(watcher, avatar, token)
            createRelationship(relation, watcherNode, repo)
            Logger.debug("Indexing user " + watcher + " due to " + relation + " with " + repo.getProperty("name"))
            indexUser(watcher, None, depth + 1, maxDepth, token)
          }
          // let see if there a pagination by looking at Link header's param.
          response.header("Link") match {
            case Some(link) => {
              var links: Array[String] = link.split(",")
              links = links.filter(link =>
                if (link.contains("rel=\"next\"")) {
                  true
                } else {
                  false
                }
              )
              if (links.size > 0) {
                val nextUrl: String = links(0).split(";")(0).replace("<", "").replace(">", "")
                Logger.debug("There is some user stuff to do, so calling url " + nextUrl)
                indexUserFromAPIRepoReturnedRepositories(nextUrl, relation, repo, depth, maxDepth, token)
              }
            }
          }
        }
    }
  }

  /**
   * Index repositories from a repository call of an user.
   *
   * @param url
   * @param login
   * @param depth
   * @param maxDepth
   */
  def indexRepoFromAPIUserReturnRepositories(url: String, login: String, relation: String, depth: Int, maxDepth: Int, token: String) {
    val user :Node = getOrSaveUser(login, None, token)
    val futurereposResp: Future[Response] = getGithubResponse(url, user)
    futurereposResp.map {
      response =>
        Logger.debug("Github response code is : " + response.status + " for " + url)
        if (response.status == 200) {
          val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
          Logger.debug("Url " + url + " has " + json.size + " repositories")
          for (jsObject <- json) {
            val repo: String = jsObject.\("name").toString().replace("\"", "")
            val owner: String = jsObject.\("owner").\("login").toString().replace("\"", "")
            val avatar: Option[String] = Some(jsObject.\("owner").\("avatar_url").toString().replace("\"", ""))
            val description: Option[String] = Some(jsObject.\("description").toString().replace("\"", ""))
            val repoNode: Node = getOrSaveRepo(owner, repo, description, token)
            val userNode: Node = getOrSaveUser(login, avatar, token)
            createRelationship(relation, userNode, repoNode)
            Logger.debug("Indexing repo " + owner + "/" + repo + " due to its " + relation + " with " + login)
            indexRepo(owner, repo, depth + 1, maxDepth, token)
          }
          // let see if there a pagination by looking at Link header's param.
          response.header("Link") match {
            case Some(link) => {
              var links: Array[String] = link.split(",")
              links = links.filter(link =>
                if (link.contains("rel=\"next\"")) {
                  true
                } else {
                  false
                }
              )
              if (links.size > 0) {
                val nextUrl: String = links(0).split(";")(0).replace("<", "").replace(">", "")
                Logger.debug("There is some other stuff to do, so calling url " + nextUrl)
                indexRepoFromAPIUserReturnRepositories(nextUrl, login, relation, depth, maxDepth, token)
              }
            }
          }
        }
    }
  }

  /**
   * Index user from a user api call that return user (like follower & following).
   *
   * @param url
   * @param login
   * @param depth
   * @param maxDepth
   */
  def indexUserFromAPIUserReturnUser(url: String, relation: String, login: String, depth: Int, maxDepth: Int, token: String) {
    val user :Node = getOrSaveUser(login, None, token)
    val futureReposResp: Future[Response] = getGithubResponse(url, user)
    futureReposResp.map {
      response =>
        Logger.debug("Github response code is : " + response.status + " for " + url)
        if (response.status == 200) {
          val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
          Logger.debug("User " + login + " " + relation + " " + json.size + " user")
          for (jsObject <- json) {
            val following: String = jsObject.\("login").toString().replace("\"", "")
            val avatar: Option[String] = Some(jsObject.\("avatar_url").toString().replace("\"", ""))
            val followingNode: Node = getOrSaveUser(following, avatar, token)
            if (relation.equalsIgnoreCase(REL_FOLLOW)) {
              createRelationship(REL_FOLLOW, followingNode, user)
            } else {
              createRelationship(REL_FOLLOW, user, followingNode)
            }
            Logger.debug("Indexing user " + following + " due to " + relation + " with " + login)
            indexUser(following, avatar, depth + 1, maxDepth, token)
          }
          // let see if there a pagination by looking at Link header's param.
          response.header("Link") match {
            case Some(link) => {
              var links: Array[String] = link.split(",")
              links = links.filter(link =>
                if (link.contains("rel=\"next\"")) {
                  true
                } else {
                  false
                }
              )
              if (links.size > 0) {
                val nextUrl: String = links(0).split(";")(0).replace("<", "").replace(">", "")
                Logger.debug("There is some user stuff to do, so calling url " + nextUrl)
                indexUserFromAPIUserReturnUser(nextUrl, relation, login, depth, maxDepth, token)
              }
            }
          }
        }
    }
  }

  /**
   * Save a user into database (if it not already exist).
   *
   * @param login
   * @return user node
   */
  def getOrSaveUser(login: String, avatar:Option[String], token :String): Node = {
    val index: Index[Node] = Neo4j.graphDb.index().forNodes(NODE_USER)
    val url: String = GITHUB_HTML_URL + "/" + login
    // if there is nothing in the indexes
    if (index.get("login", login).size() == 0) {
      Logger.debug("Creating user " + login)
      val tx: Transaction = Neo4j.graphDb.beginTx()
      try {

        // creating repository node
        val user: Node = Neo4j.graphDb.createNode()
        user.setProperty("login", login)
        user.setProperty("url", url)
        user.setProperty("indexed", false)

        avatar match {
          case Some(avatar) => {
            Logger.debug("Avatar url for " + login + " is " + avatar)
            user.setProperty("avatar", avatar)
          }
          case None => {
            val userUrl = GITHUB_API_URL + "/users/" + login + "?" + githubAuthParam(token)
            val futureReposResp: Future[Response] = WS.url(userUrl).get()
            futureReposResp.map {
              response =>
                Logger.debug("Github response code is : " + response.status + " for " + userUrl)
                if (response.status == 200) {
                  val json: JsValue = response.json
                  val avatar: String = json.\("avatar_url").toString().replace("\"", "")
                  Logger.debug("Avatar url for " + login + " is " + avatar)
                  val innerTx: Transaction = Neo4j.graphDb.beginTx()
                  user.setProperty("avatar", avatar)
                  innerTx.success()
                  innerTx.finish()
                }
            }
          }
        }

        // linked node to master repository node
        val root: Node = Neo4j.graphDb.getReferenceNode
        val masterNode: Node = root.getSingleRelationship(DynamicRelationshipType.withName(NODE_USER), Direction.OUTGOING).getEndNode
        masterNode.createRelationshipTo(user, DynamicRelationshipType.withName(NODE_USER))

        // index properties node
        index.add(user, "login", login)
        index.add(user, "url", url)

        // commit
        tx.success()

        // return node
        user
      } catch {
        case e: Exception => {
          Logger.error("Error when creating user node", e)
          throw e
        }
      } finally {
        tx.finish()
      }

    }
    else {
      index.get("login", login).getSingle
    }
  }

  /**
   * Save a repository into database (if it not already exist), and link it to the user.
   *
   * @param user
   * @param reposiroty
   * @return repository node
   */
  def getOrSaveRepo(user: String, reposiroty: String, description :Option[String], token :String): Node = {
    val name: String = user + "/" + reposiroty
    val index: Index[Node] = Neo4j.graphDb.index().forNodes(NODE_REPOSITORY)
    val url: String = GITHUB_HTML_URL + "/" + user + "/" + reposiroty
    // if there is nothing in the indexes
    if (index.get("name", name).size() == 0) {
      Logger.debug("Creating repo " + reposiroty + " by " + user)
      val tx: Transaction = Neo4j.graphDb.beginTx()
      try {

        // creating repository node
        val repo: Node = Neo4j.graphDb.createNode()
        repo.setProperty("name", name)
        repo.setProperty("user", user)
        repo.setProperty("repository", reposiroty)
        repo.setProperty("url", url)
        repo.setProperty("indexed", false)
        description match {
          case Some(description) => repo.setProperty("description", description)
          case None => {
            val repoUrl = GITHUB_API_URL + "/repos/" + user + "/" + reposiroty + "?" + githubAuthParam(token)
            val futureReposResp: Future[Response] = WS.url(repoUrl).get()
            futureReposResp.map {
              response =>
                Logger.debug("Github response code is : " + response.status + " for " + repoUrl)
                if (response.status == 200) {
                  val json: JsValue = response.json
                  val description: String = json.\("description").toString().replace("\"", "")
                  val innerTx: Transaction = Neo4j.graphDb.beginTx()
                  repo.setProperty("description", description)
                  innerTx.success()
                  innerTx.finish()
                }
            }
          }
        }

        // linked node to master repository node
        val root: Node = Neo4j.graphDb.getReferenceNode
        val masterNode: Node = root.getSingleRelationship(DynamicRelationshipType.withName(NODE_REPOSITORY), Direction.OUTGOING).getEndNode
        masterNode.createRelationshipTo(repo, DynamicRelationshipType.withName(NODE_REPOSITORY))

        // index properties node
        index.add(repo, "name", name)
        index.add(repo, "user", user)
        index.add(repo, "repository", reposiroty)

        val owner: Node = getOrSaveUser(user, None, token)
        createRelationship(REL_CREATE, owner, repo)


        // commit
        tx.success()

        // return node
        repo
      } catch {
        case e: Exception => {
          Logger.error("Error when creating relationship", e)
          throw e
        }
      } finally {
        tx.finish()
      }

    }
    else {
      index.get("name", name).getSingle
    }
  }

  /**
   * Helper to create a relationship between two node.
   *
   * @param name
   * @param from
   * @param to
   */
  def createRelationship(name: String, from: Node, to: Node) {
    val relations: Iterable[Relationship] = from.getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName(name)).asScala
    val relFromTo = relations.filter(relation => {
      if (relation.getEndNode.getId == to.getId) {
        true
      } else {
        false
      }
    })
    if (relFromTo.size == 0) {
      Logger.debug("Create relationship " + name + " between " + from.getProperty("url") + " and " + to.getProperty("url"))
      val tx: Transaction = Neo4j.graphDb.beginTx()
      try {
        from.createRelationshipTo(to, DynamicRelationshipType.withName(name))
        tx.success()
      } catch {
        case e: Exception => {
          Logger.error("Error when creating relationship", e)
          throw e
        }
      } finally {
        tx.finish()
      }
    }
  }

  /**
   * Getting github response from url. Adding "if modified since header", if node has been already indexed to prevent API rate limit.
   *
   * @param url
   * @param node
   * @return
   */
  def getGithubResponse(url: String, node: Node): Future[Response] = {
    if (isIndexed(node)) {
      WS.url(url).withHeaders("If-Modified-Since" -> getUpdateDate(node)).get()
    }
    else {
      WS.url(url).get()
    }
  }

  /**
   * Set to status 'indexed' a node.
   *
   * @param node
   */
  def setIndexed(node: Node) {
    val tx: Transaction = Neo4j.graphDb.beginTx()
    try {
      node.setProperty("updated", new Date().getTime)
      node.setProperty("indexed", true)
      tx.success()
    } catch {
      case e: Exception => {
        Logger.error("Error when setting indexed", e)
        throw e
      }
    } finally {
      tx.finish()
    }
  }

  /**
   * Return true if the node is already indexed.
   *
   * @param node
   * @return
   */
  def isIndexed(node: Node): Boolean = {
    val isIndexed: Boolean = node.getProperty("indexed", false).asInstanceOf[Boolean]
    isIndexed
  }

  /**
  * Retrieve the updated date of a node.
  *
  * @param node
  * @return
  */
  def getUpdateDate(node: Node): String = {
    val time: Long = node.getProperty("updated", 0.asInstanceOf[Long]).asInstanceOf[Long]
    val date: Date = new Date(time)
    val format: SimpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")
    format.format(date)
  }

  /**
   * Is node should be indexed ?
   */
  def shouldIndex(node :Node) :Boolean = {
    val time: Long = node.getProperty("updated", 0.asInstanceOf[Long]).asInstanceOf[Long]
    val currentTime :Long = new Date().getTime
    if( (currentTime - time) < (1000 * 60 * 60 * 24)){
      Logger.debug("Won't index node " + node.getId)
      false
    } else {
      Logger.debug("Index node " + node.getId)
      true
    }
  }

  /**
   * Adding github param for auth.
   *
   * @param token
   * @return
   */
  def githubAuthParam(token: String): String = {
    val params: String = "access_token=" + token
    params
  }

  def getUserReco(name :String) :List[JsObject] ={
    var users :List[JsObject] = List[JsObject]()
    var recos :List[JsObject] = List[JsObject]()
    val engine :ExecutionEngine = new ExecutionEngine(Neo4j.graphDb);
    val query :String = "" +
      "START " +
      "me=node:USER(login=\"" + name + "\") " +
      "MATCH " +
      "me-[:FOLLOW]->friend-[:FOLLOW]->friend_of_friend, " +
      "me-[r?:FOLLOW]->friend_of_friend " +
      "WHERE " +
      "(r IS NULL) AND " +
      "friend_of_friend <> me " +
      "RETURN " +
      "friend_of_friend.login, friend_of_friend.avatar?, COUNT(*) " +
      "ORDER BY " +
      "COUNT(*) DESC " +
      "LIMIT 10"
    Logger.debug(query)
    val result :ExecutionResult = engine.execute(query);
    result.foreach( row => {
      users = Json.obj(
          "login" -> row.getOrElse("friend_of_friend.login", "").toString,
          "avatar" -> row.getOrElse("friend_of_friend.avatar?", "").toString
        ) :: users
    })
    // a little random on the 10 first item to not show the same reco every time
    if(users.length > 0) {
      val rand = new Random(System.currentTimeMillis())
      var random_index = 0
      while(recos.length < 3){
        random_index = rand.nextInt(users.length)
        if(!recos.contains(users(random_index))){
          recos = users(random_index) :: recos
        }
      }
    }
    recos
  }

  def getRepositoryReco(login :Option[String], name :String) :List[JsObject] = {
    var repos :List[JsObject] = List()
    var recos :List[JsObject] = List()
    val engine :ExecutionEngine = new ExecutionEngine(Neo4j.graphDb);
    val query :String = login match {
      case Some(login) => {
        "START " +
          "repo=node:REPOSITORY(name=\"" + name + "\"), " +
          "me=node:USER(login=\"" + login + "\") " +
        "MATCH " +
          "contributors-[:HAS_CONTRIBUTED]->repo, " +
          "contributors-[:STARE]->repos, " +
          "me-[r?:STARE]->repos ," +
          "creator-[:HAS_CREATED]->repos " +
        "WHERE " +
          "repos <> repo AND "
          "r IS NULL " +
        "RETURN " +
          "creator.login, creator.avatar?, repos.repository, repos.description?, COUNT(*) " +
        "ORDER BY " +
          "COUNT(*) DESC " +
        "LIMIT 10"
      }
      case None => {
        "START " +
          "repo=node:REPOSITORY(name=\"" + name + "\") " +
        "MATCH " +
          "contributors-[:HAS_CONTRIBUTED]->repo, " +
          "contributors-[:STARE]->repos, " +
          "creator-[:HAS_CREATED]->repos " +
        "WHERE " +
          "repo <> repos " +
        "RETURN " +
          "creator.login, creator.avatar?, repos.repository, repos.description?, COUNT(*) " +
        "ORDER BY " +
          "COUNT(*) DESC " +
        "LIMIT 10"
      }
    }
    Logger.debug(query)
    val result :ExecutionResult = engine.execute(query)
    result.foreach( row => {
      repos = Json.obj(
        "owner" -> row.getOrElse("creator.login", "").toString,
        "avatar" -> row.getOrElse("creator.avatar?", "").toString,
        "repository" -> row.getOrElse("repos.repository", "").toString,
        "description" -> row.getOrElse("repos.description?", "").toString
      ) :: repos
    })
    // a little random on the 10 first item to not show the same reco every time
    if(repos.length > 0){
      val rand = new Random(System.currentTimeMillis())
      var random_index = 0
      while(recos.length < 3){
        random_index = rand.nextInt(repos.length)
        if(!recos.contains(repos(random_index))){
          recos = repos(random_index) :: recos
        }
      }
    }
    recos
  }
}
