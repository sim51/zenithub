package services

import org.neo4j.graphdb._
import org.neo4j.graphdb.index.Index
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.{WS, Response}
import scala.collection.JavaConverters._
import ExecutionContext.Implicits.global
import play.api.Logger
import org.neo4j.cypher.{ExecutionResult, ExecutionEngine}
import play.api.libs.ws.Response
import play.api.libs.json.JsObject

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
  val REL_FORK: String = "HAS_FORKED"

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

  /**
   * Index github user into neo4j database.
   *
   * @param login
   */
  def indexUser(login: String, token: String, depth: Int, maxDepth: Int) {
    Logger.debug("Indexing github user " + login)
    // Create nodes
    val user: Node = getOrSaveUser(login)

    if (depth < maxDepth) {

      // get all follower : create relation and index user
      var url :String = GITHUB_API_URL + "/users/" + login + "/followers?per_page=100&access_token=" + token
      indexUserFromAPIUserReturnUser(url, REL_FOLLOW, login, token, depth, maxDepth)

      // get all following : create relation and index user
      url = GITHUB_API_URL + "/users/" + login + "/following?per_page=100&access_token=" + token
      indexUserFromAPIUserReturnUser(url, "following", login, token, depth, maxDepth)

      // get all user repo : create relation and index repo
      url = GITHUB_API_URL + "/users/" + login + "/repos?per_page=100&access_token=" + token
      indexRepoFromAPIUserReturnRepositories(url, login, REL_CREATE, token,  depth, maxDepth)

      // get all stared repo by the user : create relation and index repo
      url = GITHUB_API_URL + "/users/" + login + "/starred?per_page=100&access_token=" + token
      indexRepoFromAPIUserReturnRepositories(url, login, REL_STARE, token,  depth, maxDepth)

      // get all watch repo by the user: create relation and index repo
      url = GITHUB_API_URL + "/users/" + login + "/subscriptions?per_page=100&access_token=" + token
      indexRepoFromAPIUserReturnRepositories(url, login, REL_WATCHER, token,  depth, maxDepth)

    }
  }

  /**
   * Index github repo into neo4j database.
   *
   * @param login
   * @param repository
   * @param token
   */
  def indexRepo(login: String, repository: String, token: String, depth: Int, maxDepth: Int) {
    Logger.debug("Indexing github repo " + repository + " by " + login)
    // Create nodes
    val repo: Node = getOrSaveRepo(login, repository)

    if (depth < maxDepth) {

      Logger.debug("Going deeper")

      // get all forks : create relation and index user
      var url: String = GITHUB_API_URL + "/repos/" + login + "/" + repository + "/forks?per_page=1000&access_token=" + token
      indexUserFromAPIRepoReturnedRepositories(url, REL_FORK, repo, token, depth, maxDepth)

      // get all watcher : create relation and index user
      url = GITHUB_API_URL + "/repos/" + login + "/" + repository + "/subscribers?per_page=1000&access_token=" + token
      indexUserFromAPIRepoReturnedUser(url: String, REL_WATCHER, repo, token, depth, maxDepth)

      // get all stares : create relation and index user
      url = GITHUB_API_URL + "/repos/" + login + "/" + repository + "/stargazers?per_page=1000&access_token=" + token
      indexUserFromAPIRepoReturnedUser(url: String, REL_STARE, repo, token, depth, maxDepth)

      // get all contributors : create relation and index user
      url = GITHUB_API_URL + "/repos/" + login + "/" + repository + "/contributors?per_page=1000&access_token=" + token
      indexUserFromAPIRepoReturnedUser(url: String, REL_CONTRIBUTOR, repo, token, depth, maxDepth)
    }
  }

  /**
   * Index users (ie watchers, stares, contributors) from a repository API call that return user.
   *
   * @param url
   * @param relation
   * @param repo
   * @param token
   */
  def indexUserFromAPIRepoReturnedUser(url: String, relation: String, repo: Node, token: String, depth: Int, maxDepth: Int) {
    val futureResp: Future[Response] = WS.url(url).get()
    futureResp.map { response =>
      Logger.debug("Github response code is : " + response.status)
      if (response.status == 200) {
        val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
        Logger.debug("Url " + url + " has " + json.size + " relation of type " + relation)
        for (jsObject <- json) {
          val login: String = jsObject.\("login").toString().replace("\"", "")
          val user: Node = getOrSaveUser(login)
          createRelationship("stars", user, repo)
          indexUser(login, token, depth + 1, maxDepth)
        }
        response.header("Link") match {
          case Some(link) => {
            var links :Array[String] = link.split(",")
            links = links.filter( link =>
              if(link.contains("rel=\"next\"")){
                true
              } else{
                false
              }
            )
            if (links.size > 0) {
              val nextUrl :String = links(0).split(";")(0).replace("<", "").replace(">", "")
              Logger.debug("There is some user stuff to do, SO calling url " + nextUrl)
              indexUserFromAPIRepoReturnedUser(nextUrl, relation, repo, token, depth, maxDepth)
            }
          }
        }
      }
    }
  }

  /**
   * Index users (ie watchers, stares, contributors) from a repository API call that return repositories.
   *
   * @param url
   * @param relation
   * @param repo
   * @param token
   */
  def indexUserFromAPIRepoReturnedRepositories(url: String, relation: String, repo: Node, token: String, depth: Int, maxDepth: Int) {
    val futureResp: Future[Response] = WS.url(url).get()
    futureResp.map { response =>
      Logger.debug("Github response code is : " + response.status)
      if (response.status == 200) {
        val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
        Logger.debug("Url " + url + " has " + json.size + " relation of type " + relation)
        for (jsObject <- json) {
          val watcher: String = jsObject.\("owner").\("login").toString().replace("\"", "")
          val watcherNode: Node = getOrSaveUser(watcher)
          createRelationship(relation, watcherNode, repo)
          indexUser(watcher, token, depth + 1, maxDepth)
        }
        // let see if there a pagination by looking at Link header's param.
        response.header("Link") match {
          case Some(link) => {
            var links :Array[String] = link.split(",")
            links = links.filter( link =>
              if(link.contains("rel=\"next\"")){
                true
              } else{
                false
              }
            )
            if (links.size > 0) {
              val nextUrl :String = links(0).split(";")(0).replace("<", "").replace(">", "")
              Logger.debug("There is some user stuff to do, so calling url " + nextUrl)
              indexUserFromAPIRepoReturnedRepositories(nextUrl, relation, repo, token, depth, maxDepth)
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
   * @param token
   * @param depth
   * @param maxDepth
   */
  def indexRepoFromAPIUserReturnRepositories(url: String, login: String, relation :String, token: String, depth: Int, maxDepth: Int) {
    val futurereposResp: Future[Response] = WS.url(url).get()
    futurereposResp.map { response =>
      Logger.debug("Github response code is : " + response.status)
      if (response.status == 200) {
        val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
        Logger.debug("Url " + url + " has " + json.size + " repositories")
        for (jsObject <- json) {
          val repo: String = jsObject.\("name").toString().replace("\"", "")
          val owner: String = jsObject.\("owner").\("login").toString().replace("\"", "")
          val repoNode :Node = getOrSaveRepo(owner, repo)
          val userNode :Node = getOrSaveUser(login)
          createRelationship(relation, userNode, repoNode)
          indexRepo(login, repo, token, depth + 1, maxDepth)
        }
        // let see if there a pagination by looking at Link header's param.
        response.header("Link") match {
          case Some(link) => {
            var links :Array[String] = link.split(",")
            links = links.filter( link =>
              if(link.contains("rel=\"next\"")){
                true
              } else{
                false
              }
            )
            if (links.size > 0) {
              val nextUrl :String = links(0).split(";")(0).replace("<", "").replace(">", "")
              Logger.debug("There is some user stuff to do, so calling url " + nextUrl)
              indexRepoFromAPIUserReturnRepositories(nextUrl, login, relation, token, depth, maxDepth)
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
   * @param token
   * @param depth
   * @param maxDepth
   */
  def indexUserFromAPIUserReturnUser(url: String, relation :String, login: String, token: String, depth: Int, maxDepth: Int) {
    val futurereposResp: Future[Response] = WS.url(url).get()
    futurereposResp.map { response =>
      Logger.debug("Github response code is : " + response.status)
      if (response.status == 200) {
        val json: Seq[JsObject] = response.json.as[Seq[JsObject]]
        Logger.debug("User " + login + " " + relation + " " + json.size + " user")
        for (jsObject <- json) {
          val following: String = jsObject.\("login").toString().replace("\"", "")
          val followingNode: Node = getOrSaveUser(following)
          val user: Node = getOrSaveUser(login)
          if(relation.equalsIgnoreCase(REL_FOLLOW)){
            createRelationship(REL_FOLLOW, followingNode, user)
          } else {
            createRelationship(REL_FOLLOW, user, followingNode)
          }
          indexUser(following, token, depth + 1, maxDepth)
        }
        // let see if there a pagination by looking at Link header's param.
        response.header("Link") match {
          case Some(link) => {
            var links :Array[String] = link.split(",")
            links = links.filter( link =>
              if(link.contains("rel=\"next\"")){
                true
              } else{
                false
              }
            )
            if (links.size > 0) {
              val nextUrl :String = links(0).split(";")(0).replace("<", "").replace(">", "")
              Logger.debug("There is some user stuff to do, so calling url " + nextUrl)
              indexUserFromAPIUserReturnUser(nextUrl, relation, login, token, depth, maxDepth)
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
  def getOrSaveUser(login: String): Node = {
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
          Logger.error("Error when creating relationship", e)
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
  def getOrSaveRepo(user: String, reposiroty: String): Node = {
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

        // linked node to master repository node
        val root: Node = Neo4j.graphDb.getReferenceNode
        val masterNode: Node = root.getSingleRelationship(DynamicRelationshipType.withName(NODE_REPOSITORY), Direction.OUTGOING).getEndNode
        masterNode.createRelationshipTo(repo, DynamicRelationshipType.withName(NODE_REPOSITORY))

        // index properties node
        index.add(repo, "name", name)
        index.add(repo, "user", user)
        index.add(repo, "repository", reposiroty)

        val owner: Node = getOrSaveUser(user)
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
    val relations :Iterable[Relationship] = from.getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName(name)).asScala
    val relFromTo = relations.filter(relation => {
      if (relation.getEndNode.getId == to.getId) {
        true
      } else {
        false
      }
    })
    if(relFromTo.size == 0) {
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

}
