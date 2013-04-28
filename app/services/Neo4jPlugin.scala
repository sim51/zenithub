package services

import play.api.{Logger, Plugin, Application}

/**
 * Neo4j plugin to stop server.
 * @author : bsimard
 */
class Neo4jPlugin(app: play.api.Application) extends Plugin {

  override def onStart{
    Logger.debug("Starting neo4j plugin")
    Neo4j.initDb()
  }

  override def onStop(){
    Logger.debug("Stopping neo4j plugin")
    Neo4j.stop()
  }
}
