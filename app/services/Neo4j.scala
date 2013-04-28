package services

import play.api.Play
import play.Logger
import play.api.Play.current
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.server.WrappingNeoServerBootstrapper

/**
 * Neo4j database object.
 *
 * @author : bsimard
 */
object Neo4j {

  /**
   * Graph database
   */
  var graphDb :GraphDatabaseService = start()

  /**
   * Server for webadmin
   */
  var webadmin :WrappingNeoServerBootstrapper = startAdmin()

  /**
   * Starting neo4j database
   *
   * @return
   */
  def start() :GraphDatabaseService = {
    val DBPath :String = Play.configuration.getString("neo4j.path").getOrElse("neo4j");
    Logger.debug("Neo4j database path is :" + DBPath);
    new GraphDatabaseFactory().newEmbeddedDatabase(DBPath)
  }

  /**
   * Starting webadmin server
   *
   * @return
   */
  def startAdmin() :WrappingNeoServerBootstrapper = {
    val bootstrapper :WrappingNeoServerBootstrapper =  new WrappingNeoServerBootstrapper(graphDb.asInstanceOf[GraphDatabaseAPI])
    bootstrapper.start()
    bootstrapper
  }

  /**
   * Stopping server & admin server
   */
  def stop() {
    webadmin.stop()
    graphDb.shutdown()
  }

  /**
   * Init the database with primary nodes
   */
  def initDb() {
    Logger.debug("Init database")
    val tx :Transaction = graphDb.beginTx()
    try {
      val root :Node = graphDb.getReferenceNode
      // User relationShip
      if (!root.hasRelationship(DynamicRelationshipType.withName(IndexGithub.NODE_USER), Direction.OUTGOING)){
        val userNode :Node= graphDb.createNode()
        graphDb.getReferenceNode.createRelationshipTo(userNode, DynamicRelationshipType.withName(IndexGithub.NODE_USER))
        Logger.debug("user root node created")
      }
      // Repository relationShip
      if (!root.hasRelationship(DynamicRelationshipType.withName(IndexGithub.NODE_REPOSITORY), Direction.OUTGOING)){
        val repositoryNode :Node= graphDb.createNode()
        graphDb.getReferenceNode.createRelationshipTo(repositoryNode, DynamicRelationshipType.withName(IndexGithub.NODE_REPOSITORY))
        Logger.debug("repository root node created")
      }
      tx.success()
    } catch {
      case e: Exception => Logger.error("Error when init database", e)
    } finally {
      tx.finish()
    }
  }

}
