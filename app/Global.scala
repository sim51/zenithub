import play.api._

/**
 * Global settings for the application.
 */
object Global extends GlobalSettings {

  /**
   * What we do when application start ?
   * @param app
   */
  override def onStart(app: Application) {
    Logger.info("Application has started")
  }

  /**
   * What we do when application stop ?
   * @param app
   */
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}