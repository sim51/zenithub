package services

import org.neo4j.graphdb.GraphDatabaseService
import play.api.{Play, PlayException}
import play.Logger
import play.api.Play.current
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.GraphDatabaseAPI
import org.neo4j.server.WrappingNeoServerBootstrapper

/**
 * Neo4j database object.
 *
 * @author : bsimard
 */
object Neo4j {

  var graphDb :GraphDatabaseService = start()
  var webadmin :WrappingNeoServerBootstrapper = startAdmin()

  def start() :GraphDatabaseService = {
    val DBPath :String = Play.configuration.getString("neo4j.path").getOrElse("neo4j");
    Logger.debug("Neo4j database path is :" + DBPath);
    new GraphDatabaseFactory().newEmbeddedDatabase(DBPath)
  }

  def startAdmin() :WrappingNeoServerBootstrapper = {
    var bootstrapper :WrappingNeoServerBootstrapper =  new WrappingNeoServerBootstrapper(graphDb.asInstanceOf[GraphDatabaseAPI])
    bootstrapper.start()
    bootstrapper
  }

  def stop(){
    webadmin.stop()
    graphDb.shutdown()
  }

}
